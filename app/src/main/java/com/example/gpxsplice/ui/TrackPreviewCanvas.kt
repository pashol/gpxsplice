package com.example.gpxsplice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.SplitResult
import com.example.gpxsplice.domain.orderedPoints

internal fun previewColors(colorScheme: ColorScheme): List<Color> = listOf(
    colorScheme.primary,
    colorScheme.secondary,
    colorScheme.tertiary,
    colorScheme.error,
    colorScheme.outline,
)

internal data class PreviewGeometry(
    val polylines: List<List<Offset>>,
    val points: List<Offset>,
)

@Composable
fun TrackPreviewCanvas(results: List<SplitResult>, modifier: Modifier = Modifier) {
    val previewColors = previewColors(MaterialTheme.colorScheme)
    Canvas(modifier = modifier) {
        buildPreviewGeometry(results, size).forEachIndexed { resultIndex, geometry ->
            val color = previewColors[resultIndex % previewColors.size]
            geometry.polylines.forEach { polyline ->
                val path = Path().apply {
                    polyline.forEachIndexed { pointIndex, point ->
                        if (pointIndex == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }
                drawPath(path = path, color = color, style = Stroke(width = 5f))
            }
            geometry.points.forEach { point ->
                drawCircle(color = color, radius = 6f, center = point)
            }
        }
    }
}

internal fun buildPreviewGeometry(results: List<SplitResult>, canvasSize: Size): List<PreviewGeometry> {
    val allPoints = results.flatMap { it.document.orderedPoints() }
    if (allPoints.isEmpty()) return emptyList()

    val minLat = allPoints.minOf { it.latitude }
    val maxLat = allPoints.maxOf { it.latitude }
    val minLon = allPoints.minOf { it.longitude }
    val maxLon = allPoints.maxOf { it.longitude }
    val latRange = (maxLat - minLat).takeIf { it != 0.0 } ?: 1.0
    val lonRange = (maxLon - minLon).takeIf { it != 0.0 } ?: 1.0

    fun project(point: com.example.gpxsplice.domain.TrackPoint): Offset {
        val x = ((point.longitude - minLon) / lonRange).toFloat() * canvasSize.width
        val y = canvasSize.height - ((point.latitude - minLat) / latRange).toFloat() * canvasSize.height
        return Offset(x, y)
    }

    return results.map { result ->
        buildPreviewGeometry(result.document, ::project)
    }
}

private fun buildPreviewGeometry(document: GpxDocument, project: (com.example.gpxsplice.domain.TrackPoint) -> Offset): PreviewGeometry {
    val polylines = mutableListOf<List<Offset>>()
    val points = mutableListOf<Offset>()

    document.tracks.forEach { track ->
        track.segments.forEach { segment ->
            val projectedPoints = segment.points.map(project)
            when {
                projectedPoints.size > 1 -> polylines += projectedPoints
                projectedPoints.size == 1 -> points += projectedPoints.single()
            }
        }
    }

    return PreviewGeometry(polylines = polylines, points = points)
}
