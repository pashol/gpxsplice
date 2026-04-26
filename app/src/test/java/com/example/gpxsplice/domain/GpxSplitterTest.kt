package com.example.gpxsplice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxSplitterTest {
    private fun documentWithPoints(count: Int): GpxDocument {
        val points = (0 until count).map { index ->
            TrackPoint(latitude = 52.0, longitude = 5.0 + index * 0.01)
        }
        return GpxDocument("fixture", listOf(Track("track", listOf(TrackSegment(points)))))
    }

    @Test
    fun splitsByMaxPoints() {
        val results = GpxSplitter.split(
            documentWithPoints(5),
            SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 2),
        )

        assertEquals(listOf(1, 2, 3), results.map { it.index })
        assertEquals(listOf(2, 2, 1), results.map { it.pointCount })
    }

    @Test
    fun splitsByEqualStages() {
        val results = GpxSplitter.split(
            documentWithPoints(10),
            SplitOptions(mode = SplitMode.EQUAL_STAGES, stages = 3),
        )

        assertEquals(listOf(4, 3, 3), results.map { it.pointCount })
    }

    @Test
    fun splitsByDistanceWithoutInterpolation() {
        val document = documentWithPoints(4)
        val results = GpxSplitter.split(
            document,
            SplitOptions(mode = SplitMode.DISTANCE, distanceMeters = 900.0),
        )

        assertTrue(results.size > 1)
        assertEquals(listOf(3, 2), results.map { it.pointCount })
        assertEquals(results[0].document.orderedPoints().last(), results[1].document.orderedPoints().first())
        assertEquals(document.orderedPoints().totalDistanceMeters(), results.sumOf { it.distanceMeters }, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDocumentWithoutTrackPoints() {
        GpxSplitter.split(
            GpxDocument("fixture", listOf(Track("track", listOf(TrackSegment(emptyList()))))),
            SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 1),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroMaxPoints() {
        GpxSplitter.split(documentWithPoints(2), SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTooManyStages() {
        GpxSplitter.split(documentWithPoints(2), SplitOptions(mode = SplitMode.EQUAL_STAGES, stages = 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMultipleTracks() {
        val document =
            GpxDocument(
                "fixture",
                listOf(
                    Track("track-1", listOf(TrackSegment(listOf(TrackPoint(52.0, 5.0))))),
                    Track("track-2", listOf(TrackSegment(listOf(TrackPoint(52.0, 5.01))))),
                ),
            )

        GpxSplitter.split(document, SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMultipleSegments() {
        val document =
            GpxDocument(
                "fixture",
                listOf(
                    Track(
                        "track",
                        listOf(
                            TrackSegment(listOf(TrackPoint(52.0, 5.0))),
                            TrackSegment(listOf(TrackPoint(52.0, 5.01))),
                        ),
                    ),
                ),
            )

        GpxSplitter.split(document, SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 1))
    }
}
