package com.example.gpxsplice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GpxMergerTest {
    @Test
    fun appendsTracksInInputOrder() {
        val first = document("first-doc", track("first-track", point(52.0, 5.0)))
        val second = document("second-doc", track("second-track", point(53.0, 6.0)))

        val merged = GpxMerger.merge(listOf(first, second))

        assertEquals("Merged GPX", merged.name)
        assertEquals(listOf("first-track", "second-track"), merged.tracks.map { it.name })
        assertEquals(listOf(point(52.0, 5.0), point(53.0, 6.0)), merged.orderedPoints())
    }

    @Test
    fun preservesTrackSegmentsAndPointMetadata() {
        val firstPoint = TrackPoint(52.0, 5.0, elevationMeters = 12.5, time = "2026-05-01T10:00:00Z")
        val secondPoint = TrackPoint(52.1, 5.1, elevationMeters = null, time = "2026-05-01T10:05:00Z")
        val document = GpxDocument(
            name = "source",
            tracks = listOf(
                Track(
                    name = "source-track",
                    segments = listOf(
                        TrackSegment(listOf(firstPoint)),
                        TrackSegment(listOf(secondPoint)),
                    ),
                ),
            ),
        )

        val merged = GpxMerger.merge(listOf(document, document("other", track("other-track", point(53.0, 6.0)))))

        assertEquals(2, merged.tracks.first().segments.size)
        assertEquals(listOf(firstPoint), merged.tracks.first().segments[0].points)
        assertEquals(listOf(secondPoint), merged.tracks.first().segments[1].points)
    }

    @Test
    fun rejectsEmptyInput() {
        assertRejectsIllegalArgument("At least 2 GPX files are required") {
            GpxMerger.merge(emptyList())
        }
    }

    @Test
    fun rejectsSingleDocumentInput() {
        assertRejectsIllegalArgument("At least 2 GPX files are required") {
            GpxMerger.merge(listOf(document("only", track("only-track", point(52.0, 5.0)))))
        }
    }

    private fun document(name: String, vararg tracks: Track): GpxDocument =
        GpxDocument(name = name, tracks = tracks.toList())

    private fun track(name: String, vararg points: TrackPoint): Track =
        Track(name = name, segments = listOf(TrackSegment(points.toList())))

    private fun point(latitude: Double, longitude: Double): TrackPoint =
        TrackPoint(latitude = latitude, longitude = longitude)

    private fun assertRejectsIllegalArgument(expectedMessage: String, block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            assertEquals(expectedMessage, error.message)
        }
    }
}
