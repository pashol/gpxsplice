package com.example.gpxsplice.ui

import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.Track
import com.example.gpxsplice.domain.TrackPoint
import com.example.gpxsplice.domain.TrackSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxMergeModelsTest {
    @Test
    fun earliestTimestampFindsEarliestPointTime() {
        val item = mergeInput(
            fileName = "dated.gpx",
            times = listOf("2026-05-02T10:00:00Z", "2026-05-01T09:00:00Z"),
        )

        assertEquals("2026-05-01T09:00:00Z", item.earliestTimestamp)
    }

    @Test
    fun chronologicalSortAppliesWhenEveryFileHasTimestamp() {
        val later = mergeInput("later.gpx", listOf("2026-05-02T10:00:00Z"))
        val earlier = mergeInput("earlier.gpx", listOf("2026-05-01T10:00:00Z"))

        val result = orderMergeInputs(listOf(later, earlier))

        assertEquals(listOf("earlier.gpx", "later.gpx"), result.items.map { it.fileName })
        assertTrue(result.wasChronologicallySorted)
        assertNull(result.message)
    }

    @Test
    fun chronologicalSortIsSkippedWhenAnyFileHasNoTimestamp() {
        val dated = mergeInput("dated.gpx", listOf("2026-05-01T10:00:00Z"))
        val undated = mergeInput("undated.gpx", listOf(null))

        val result = orderMergeInputs(listOf(dated, undated))

        assertEquals(listOf("dated.gpx", "undated.gpx"), result.items.map { it.fileName })
        assertFalse(result.wasChronologicallySorted)
        assertEquals("Some files have no timestamps, so selected order is preserved.", result.message)
    }

    @Test
    fun moveMergeInputUpAndDownChangesOrder() {
        val first = mergeInput("first.gpx", listOf("2026-05-01T10:00:00Z"))
        val second = mergeInput("second.gpx", listOf("2026-05-02T10:00:00Z"))
        val third = mergeInput("third.gpx", listOf("2026-05-03T10:00:00Z"))

        val movedUp = moveMergeInput(listOf(first, second, third), fromIndex = 2, direction = MergeMoveDirection.UP)
        val movedDown = moveMergeInput(movedUp, fromIndex = 1, direction = MergeMoveDirection.DOWN)

        assertEquals(listOf("first.gpx", "third.gpx", "second.gpx"), movedUp.map { it.fileName })
        assertEquals(listOf("first.gpx", "second.gpx", "third.gpx"), movedDown.map { it.fileName })
    }

    @Test
    fun moveMergeInputIgnoresOutOfRangeMoves() {
        val first = mergeInput("first.gpx", listOf("2026-05-01T10:00:00Z"))
        val second = mergeInput("second.gpx", listOf("2026-05-02T10:00:00Z"))

        assertEquals(listOf(first, second), moveMergeInput(listOf(first, second), 0, MergeMoveDirection.UP))
        assertEquals(listOf(first, second), moveMergeInput(listOf(first, second), 1, MergeMoveDirection.DOWN))
    }

    @Test
    fun mergeRequiresAtLeastTwoItemsWithTrackPoints() {
        val first = mergeInput("first.gpx", listOf("2026-05-01T10:00:00Z"))
        val second = mergeInput("second.gpx", listOf("2026-05-02T10:00:00Z"))
        val empty = MergeInput(fileName = "empty.gpx", document = GpxDocument("empty", emptyList()))

        assertFalse(canMerge(emptyList()))
        assertFalse(canMerge(listOf(first)))
        assertFalse(canMerge(listOf(first, empty)))
        assertTrue(canMerge(listOf(first, second)))
    }

    private fun mergeInput(fileName: String, times: List<String?>): MergeInput =
        MergeInput(
            fileName = fileName,
            document = GpxDocument(
                name = fileName,
                tracks = listOf(
                    Track(
                        name = fileName,
                        segments = listOf(
                            TrackSegment(
                                points = times.mapIndexed { index, time ->
                                    TrackPoint(latitude = 52.0 + index, longitude = 5.0 + index, time = time)
                                },
                            ),
                        ),
                    ),
                ),
            ),
        )
}
