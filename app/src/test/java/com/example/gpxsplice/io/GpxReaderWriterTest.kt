package com.example.gpxsplice.io

import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.Track
import com.example.gpxsplice.domain.TrackPoint
import com.example.gpxsplice.domain.TrackSegment
import com.example.gpxsplice.domain.orderedPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class GpxReaderWriterTest {
    @Test
    fun readsSimpleTrack() {
        val input = javaClass.classLoader!!.getResourceAsStream("simple-track.gpx")!!
        val document = GpxReader.read(input)

        assertEquals("Simple Track", document.name)
        assertEquals("Morning Ride", document.tracks.single().name)
        assertEquals(1, document.tracks.single().segments.size)
        assertEquals(3, document.orderedPoints().size)
        assertEquals(10.0, document.orderedPoints().first().elevationMeters!!, 0.001)
        assertNull(document.orderedPoints().last().time)
    }

    @Test
    fun readsMultipleSegmentsInDocumentOrder() {
        val input = javaClass.classLoader!!.getResourceAsStream("multi-segment-track.gpx")!!
        val document = GpxReader.read(input)

        assertEquals(2, document.tracks.single().segments.size)
        assertEquals(2, document.tracks.single().segments.first().points.size)
        assertEquals(2, document.tracks.single().segments.last().points.size)
        assertEquals(4, document.orderedPoints().size)
        assertEquals(52.02, document.orderedPoints().last().latitude, 0.001)
    }

    @Test
    fun writesReadableTrack() {
        val document = GpxDocument(
            name = "Export & Verify <doc>",
            tracks = listOf(
                Track(
                    name = "Part & 1 <north>",
                    segments = listOf(
                        TrackSegment(
                            listOf(
                                TrackPoint(52.0, 5.0, 10.0, "2026-04-26T08:00:00Z"),
                                TrackPoint(52.1, 5.1),
                            ),
                        ),
                        TrackSegment(
                            listOf(
                                TrackPoint(52.2, 5.2, 20.5, "2026-04-26T08:10:00Z"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val xml = GpxWriter.write(document)
        val reparsed = GpxReader.read(xml.byteInputStream())

        assertTrue(xml.contains("Export &amp; Verify &lt;doc&gt;"))
        assertTrue(xml.contains("Part &amp; 1 &lt;north&gt;"))
        assertEquals("Export & Verify <doc>", reparsed.name)
        assertEquals("Part & 1 <north>", reparsed.tracks.single().name)
        assertEquals(2, reparsed.tracks.single().segments.size)
        assertEquals(2, reparsed.tracks.single().segments.first().points.size)
        assertEquals(1, reparsed.tracks.single().segments.last().points.size)
        assertEquals(3, reparsed.orderedPoints().size)
        assertEquals(10.0, reparsed.orderedPoints().first().elevationMeters!!, 0.001)
        assertEquals("2026-04-26T08:00:00Z", reparsed.orderedPoints().first().time)
        assertEquals(20.5, reparsed.orderedPoints().last().elevationMeters!!, 0.001)
        assertEquals("2026-04-26T08:10:00Z", reparsed.orderedPoints().last().time)
    }

    @Test
    fun readsNonNamespacedGpxElements() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="test">
              <metadata><name>Plain</name></metadata>
              <trk>
                <name>No Namespace</name>
                <trkseg>
                  <trkpt lat="52.0" lon="5.0"><ele>7.5</ele><time>2026-04-26T08:00:00Z</time></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val document = GpxReader.read(xml.byteInputStream())

        assertEquals("Plain", document.name)
        assertEquals("No Namespace", document.tracks.single().name)
        assertEquals(7.5, document.orderedPoints().single().elevationMeters!!, 0.001)
    }

    @Test
    fun reportsMalformedCoordinateWithContext() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
              <trk>
                <trkseg>
                  <trkpt lat="oops" lon="5.0" />
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        try {
            GpxReader.read(xml.byteInputStream())
            fail("Expected malformed numeric input to throw")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("lat"))
            assertTrue(error.message!!.contains("oops"))
        }
    }

    @Test
    fun rejectsDoctypeInput() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE gpx [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
              <trk>
                <trkseg>
                  <trkpt lat="52.0" lon="5.0"><time>&xxe;</time></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        try {
            GpxReader.read(xml.byteInputStream())
            fail("Expected DOCTYPE input to be rejected")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("DOCTYPE"))
        }
    }
}
