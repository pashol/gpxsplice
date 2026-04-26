package com.example.gpxsplice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.gpxsplice.domain.SplitResult
import com.example.gpxsplice.domain.orderedPoints

private val previewColors = listOf(
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFFC62828),
    Color(0xFF6A1B9A),
    Color(0xFFEF6C00),
)

@Composable
fun TrackPreviewCanvas(results: List<SplitResult>, modifier: Modifier = Modifier) {
    val allPoints = results.flatMap { it.document.orderedPoints() }
    Canvas(modifier = modifier) {
        if (allPoints.isEmpty()) return@Canvas

        val minLat = allPoints.minOf { it.latitude }
        val maxLat = allPoints.maxOf { it.latitude }
        val minLon = allPoints.minOf { it.longitude }
        val maxLon = allPoints.maxOf { it.longitude }
        val latRange = (maxLat - minLat).takeIf { it != 0.0 } ?: 1.0
        val lonRange = (maxLon - minLon).takeIf { it != 0.0 } ?: 1.0

        results.forEachIndexed { resultIndex, result ->
            val path = Path()
            result.document.orderedPoints().forEachIndexed { pointIndex, point ->
                val x = ((point.longitude - minLon) / lonRange).toFloat() * size.width
                val y = size.height - ((point.latitude - minLat) / latRange).toFloat() * size.height
                if (pointIndex == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = previewColors[resultIndex % previewColors.size],
                style = Stroke(width = 5f),
            )
        }

        if (allPoints.size == 1) {
            drawCircle(Color(0xFF1565C0), radius = 6f, center = Offset(size.width / 2, size.height / 2))
        }
    }
}
