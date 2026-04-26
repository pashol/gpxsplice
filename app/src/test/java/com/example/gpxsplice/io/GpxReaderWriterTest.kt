package com.example.gpxsplice.io

import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.Track
import com.example.gpxsplice.domain.TrackPoint
import com.example.gpxsplice.domain.TrackSegment
import com.example.gpxsplice.domain.orderedPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GpxReaderWriterTest {
    @Test
    fun readsSimpleTrack() {
        val input = javaClass.classLoader!!.getResourceAsStream("simple-track.gpx")!!
        val document = GpxReader.read(input)

        assertEquals("Simple Track", document.name)
        assertEquals("Morning Ride", document.tracks.single().name)
        assertEquals(3, document.orderedPoints().size)
        assertEquals(10.0, document.orderedPoints().first().elevationMeters!!, 0.001)
        assertNull(document.orderedPoints().last().time)
    }

    @Test
    fun readsMultipleSegmentsInDocumentOrder() {
        val input = javaClass.classLoader!!.getResourceAsStream("multi-segment-track.gpx")!!
        val document = GpxReader.read(input)

        assertEquals(4, document.orderedPoints().size)
        assertEquals(52.02, document.orderedPoints().last().latitude, 0.001)
    }

    @Test
    fun writesReadableTrack() {
        val document = GpxDocument(
            name = "Export",
            tracks = listOf(
                Track(
                    name = "Part 1",
                    segments = listOf(
                        TrackSegment(
                            listOf(
                                TrackPoint(52.0, 5.0, 10.0, "2026-04-26T08:00:00Z"),
                                TrackPoint(52.1, 5.1),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val xml = GpxWriter.write(document)
        val reparsed = GpxReader.read(xml.byteInputStream())

        assertEquals("Export", reparsed.name)
        assertEquals(2, reparsed.orderedPoints().size)
    }
}
