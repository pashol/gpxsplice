package com.example.gpxsplice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

    @Test
    fun haversineMeters_isSymmetric() {
        val amsterdam = TrackPoint(latitude = 52.3791283, longitude = 4.8980833)
        val utrecht = TrackPoint(latitude = 52.0894444, longitude = 5.1102778)

        assertEquals(haversineMeters(amsterdam, utrecht), haversineMeters(utrecht, amsterdam), 0.0)
    }

    @Test
    fun haversineMeters_returnsFiniteDistanceForExtremeValidCoordinates() {
        val north = TrackPoint(latitude = 90.0, longitude = 0.0)
        val south = TrackPoint(latitude = -90.0, longitude = 180.0)

        val distance = haversineMeters(north, south)

        assertFalse(distance.isNaN())
        assertTrue(distance.isFinite())
        assertEquals(20_015_087.0, distance, 1_000.0)
    }

    @Test
    fun splitOptions_distanceMode_requiresPositiveDistanceOnly() {
        SplitOptions(mode = SplitMode.DISTANCE, distanceMeters = 1.0)

        assertInvalidSplitOptions {
            SplitOptions(mode = SplitMode.DISTANCE, distanceMeters = 0.0)
        }
        assertInvalidSplitOptions {
            SplitOptions(mode = SplitMode.DISTANCE, distanceMeters = 10.0, maxPoints = 1)
        }
    }

    @Test
    fun splitOptions_maxPointsMode_requiresPositiveMaxPointsOnly() {
        SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 1)

        assertInvalidSplitOptions {
            SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 0)
        }
        assertInvalidSplitOptions {
            SplitOptions(mode = SplitMode.MAX_POINTS, maxPoints = 10, stages = 2)
        }
    }

    @Test
    fun splitOptions_equalStagesMode_requiresPositiveStagesOnly() {
        SplitOptions(mode = SplitMode.EQUAL_STAGES, stages = 1)

        assertInvalidSplitOptions {
            SplitOptions(mode = SplitMode.EQUAL_STAGES, stages = 0)
        }
        assertInvalidSplitOptions {
            SplitOptions(mode = SplitMode.EQUAL_STAGES, stages = 2, distanceMeters = 10.0)
        }
    }

    private fun assertInvalidSplitOptions(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
