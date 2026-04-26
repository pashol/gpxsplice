package com.example.gpxsplice.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun haversineMeters(start: TrackPoint, end: TrackPoint): Double {
    val startLatitudeRadians = Math.toRadians(start.latitude)
    val endLatitudeRadians = Math.toRadians(end.latitude)
    val latitudeDeltaRadians = Math.toRadians(end.latitude - start.latitude)
    val longitudeDeltaRadians = Math.toRadians(end.longitude - start.longitude)

    val haversine =
        sin(latitudeDeltaRadians / 2).pow(2) +
            cos(startLatitudeRadians) * cos(endLatitudeRadians) *
            sin(longitudeDeltaRadians / 2).pow(2)
    val normalizedHaversine = haversine.coerceIn(0.0, 1.0)

    return 2 * EARTH_RADIUS_METERS * asin(sqrt(normalizedHaversine))
}
