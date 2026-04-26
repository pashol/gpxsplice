package com.example.gpxsplice.domain

import java.time.Instant

data class GpxDocument(
    val tracks: List<Track>,
    val name: String? = null,
)

data class Track(
    val segments: List<TrackSegment>,
    val name: String? = null,
)

data class TrackSegment(
    val points: List<TrackPoint>,
)

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double? = null,
    val time: Instant? = null,
)

enum class SplitMode {
    DISTANCE,
    MAX_POINTS,
    EQUAL_STAGES,
}

data class SplitOptions(
    val mode: SplitMode,
    val distanceMeters: Double? = null,
    val maxPoints: Int? = null,
    val stageCount: Int? = null,
)

data class SplitResult(
    val index: Int,
    val tracks: List<Track>,
    val pointRange: IntRange,
    val pointCount: Int,
    val distanceMeters: Double,
    val displayColorArgb: Long? = null,
)
