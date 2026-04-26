package com.example.gpxsplice.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {
    @Test
    fun haversineMeters_returnsZeroForIdenticalPoints() {
        val point = TrackPoint(latitude = 52.3791283, longitude = 4.8980833)

        assertEquals(0.0, haversineMeters(point, point), 0.0)
    }

    @Test
    fun haversineMeters_returnsApproxDistanceBetweenAmsterdamAndUtrecht() {
        val amsterdam = TrackPoint(latitude = 52.3791283, longitude = 4.8980833)
        val utrecht = TrackPoint(latitude = 52.0894444, longitude = 5.1102778)

        assertEquals(35_490.0, haversineMeters(amsterdam, utrecht), 500.0)
    }
}
