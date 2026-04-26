package com.example.gpxsplice.domain

data class GpxDocument(
    val name: String?,
    val tracks: List<Track>,
)

data class Track(
    val name: String?,
    val segments: List<TrackSegment>,
)

data class TrackSegment(
    val points: List<TrackPoint>,
)

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double? = null,
    val time: String? = null,
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
    val stages: Int? = null,
)

data class SplitResult(
    val index: Int,
    val document: GpxDocument,
    val pointCount: Int,
    val distanceMeters: Double,
)
