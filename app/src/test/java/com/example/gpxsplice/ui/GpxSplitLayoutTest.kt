package com.example.gpxsplice.ui

import androidx.compose.ui.unit.dp
import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.Track
import com.example.gpxsplice.domain.TrackPoint
import com.example.gpxsplice.domain.TrackSegment
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxSplitLayoutTest {
    @Test
    fun exportActionsStackOnlyOnNarrowWidths() {
        assertTrue(shouldUseVerticalExportActions(480.dp))
        assertFalse(shouldUseVerticalExportActions(600.dp))
        assertFalse(shouldUseVerticalExportActions(840.dp))
    }

    @Test
    fun mergedPreviewResultWrapsMergedDocumentForTrackPreview() {
        val document = GpxDocument(
            name = "merged",
            tracks = listOf(
                Track(
                    name = "track",
                    segments = listOf(
                        TrackSegment(
                            points = listOf(
                                TrackPoint(latitude = 52.0, longitude = 5.0),
                                TrackPoint(latitude = 53.0, longitude = 6.0),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val result = mergedPreviewResult(document)

        assertEquals(1, result.index)
        assertSame(document, result.document)
        assertEquals(2, result.pointCount)
        assertEquals(0.0, result.distanceMeters, 0.0)
    }

    @Test
    fun cancelAndClearMergeJobCancelsRunningMergeWork() {
        val job = Job()

        val clearedJob = cancelAndClearMergeJob(job)

        assertTrue(job.isCancelled)
        assertNull(clearedJob)
    }
}
