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

    private fun trackPoint(longitude: Double): TrackPoint = TrackPoint(latitude = 52.0, longitude = longitude)

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

    @Test
    fun splitsMultipleTracksWhilePreservingTrackBoundaries() {
        val firstTrackPoint = trackPoint(5.0)
        val secondTrackPoint = trackPoint(5.01)
        val thirdTrackPoint = trackPoint(5.02)
        val fourthTrackPoint = trackPoint(5.03)
        val fifthTrackPoint = trackPoint(5.04)
        val document =
            GpxDocument(
                "fixture",
                listOf(
                    Track(
                        "track-1",
                        listOf(
                            TrackSegment(listOf(firstTrackPoint, secondTrackPoint)),
                            TrackSegment(listOf(thirdTrackPoint)),
                        ),
                    ),
                    Track("track-2", listOf(TrackSegment(listOf(fourthTrackPoint, fifthTrackPoint)))),
                ),
            )

        val results = GpxSplitter.split(document, SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 2))

        assertEquals(listOf(2, 2, 1), results.map { it.pointCount })
        assertEquals(listOf(listOf("track-1"), listOf("track-1", "track-2"), listOf("track-2")), results.map { result -> result.document.tracks.map(Track::name) })
        assertEquals(listOf(secondTrackPoint), results[0].document.tracks[0].segments[0].points.takeLast(1))
        assertEquals(listOf(thirdTrackPoint), results[1].document.tracks[0].segments[0].points)
        assertEquals(listOf(fourthTrackPoint), results[1].document.tracks[1].segments[0].points)
    }

    @Test
    fun splitsMultipleSegmentsWhilePreservingSegmentBoundaries() {
        val firstPoint = trackPoint(5.0)
        val secondPoint = trackPoint(5.01)
        val thirdPoint = trackPoint(5.02)
        val fourthPoint = trackPoint(5.03)
        val document =
            GpxDocument(
                "fixture",
                listOf(
                    Track(
                        "track",
                        listOf(
                            TrackSegment(listOf(firstPoint, secondPoint)),
                            TrackSegment(listOf(thirdPoint, fourthPoint)),
                        ),
                    ),
                ),
            )

        val results = GpxSplitter.split(document, SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 3))

        assertEquals(listOf(3, 1), results.map { it.pointCount })
        assertEquals(2, results[0].document.tracks.single().segments.size)
        assertEquals(listOf(firstPoint, secondPoint), results[0].document.tracks.single().segments[0].points)
        assertEquals(listOf(thirdPoint), results[0].document.tracks.single().segments[1].points)
        assertEquals(listOf(fourthPoint), results[1].document.tracks.single().segments[0].points)
    }

    @Test
    fun distanceSplitDoesNotCountCrossSegmentJump() {
        val firstPoint = trackPoint(5.0)
        val secondPoint = trackPoint(5.01)
        val thirdPoint = trackPoint(6.0)
        val fourthPoint = trackPoint(6.01)
        val document =
            GpxDocument(
                "fixture",
                listOf(
                    Track(
                        "track",
                        listOf(
                            TrackSegment(listOf(firstPoint, secondPoint)),
                            TrackSegment(listOf(thirdPoint, fourthPoint)),
                        ),
                    ),
                ),
            )

        val results = GpxSplitter.split(document, SplitOptions(mode = SplitMode.DISTANCE, distanceMeters = 1_000.0))

        assertEquals(1, results.size)
        assertEquals(4, results.single().pointCount)
        assertEquals(
            haversineMeters(firstPoint, secondPoint) + haversineMeters(thirdPoint, fourthPoint),
            results.single().distanceMeters,
            0.001,
        )
        assertEquals(2, results.single().document.tracks.single().segments.size)
    }
}
