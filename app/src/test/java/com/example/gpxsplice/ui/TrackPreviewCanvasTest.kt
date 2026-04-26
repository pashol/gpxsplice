package com.example.gpxsplice.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.SplitResult
import com.example.gpxsplice.domain.Track
import com.example.gpxsplice.domain.TrackPoint
import com.example.gpxsplice.domain.TrackSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackPreviewCanvasTest {
    @Test
    fun previewGeometryPreservesSegmentBoundariesWithinAResult() {
        val firstSegment = listOf(trackPoint(latitude = 0.0, longitude = 0.0), trackPoint(latitude = 1.0, longitude = 1.0))
        val secondSegment = listOf(trackPoint(latitude = 10.0, longitude = 10.0), trackPoint(latitude = 11.0, longitude = 11.0))
        val result = splitResult(listOf(Track("track", listOf(TrackSegment(firstSegment), TrackSegment(secondSegment)))))

        val geometry = buildPreviewGeometry(listOf(result), Size(100f, 100f)).single()

        assertEquals(2, geometry.polylines.size)
        assertOffsetsEqual(Offset(0f, 100f), geometry.polylines[0][0])
        assertOffsetsEqual(Offset(9.090909f, 90.90909f), geometry.polylines[0][1])
        assertOffsetsEqual(Offset(90.90909f, 9.090912f), geometry.polylines[1][0])
        assertOffsetsEqual(Offset(100f, 0f), geometry.polylines[1][1])
        assertEquals(emptyList<Offset>(), geometry.points)
    }

    @Test
    fun previewGeometryPromotesSinglePointSegmentsToVisibleDots() {
        val pointSegment = TrackSegment(listOf(trackPoint(latitude = 0.0, longitude = 0.0)))
        val lineSegment = TrackSegment(listOf(trackPoint(latitude = 10.0, longitude = 10.0), trackPoint(latitude = 20.0, longitude = 20.0)))
        val result = splitResult(listOf(Track("track", listOf(pointSegment, lineSegment))))

        val geometry = buildPreviewGeometry(listOf(result), Size(100f, 100f)).single()

        assertEquals(1, geometry.polylines.size)
        assertOffsetsEqual(Offset(50f, 50f), geometry.polylines.single()[0])
        assertOffsetsEqual(Offset(100f, 0f), geometry.polylines.single()[1])
        assertEquals(1, geometry.points.size)
        assertOffsetsEqual(Offset(0f, 100f), geometry.points.single())
    }

    private fun assertOffsetsEqual(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.0001f)
        assertEquals(expected.y, actual.y, 0.0001f)
    }

    private fun splitResult(tracks: List<Track>): SplitResult =
        SplitResult(
            index = 1,
            document = GpxDocument(name = "fixture", tracks = tracks),
            pointCount = tracks.sumOf { track -> track.segments.sumOf { it.points.size } },
            distanceMeters = 0.0,
        )

    private fun trackPoint(latitude: Double, longitude: Double): TrackPoint =
        TrackPoint(latitude = latitude, longitude = longitude)
}
