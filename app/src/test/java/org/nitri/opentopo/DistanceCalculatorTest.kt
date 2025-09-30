package org.nitri.opentopo

import org.junit.Assert.assertEquals
import org.junit.Test
import org.nitri.opentopo.util.DistanceCalculator

class DistanceCalculatorTest {

    @Test
    fun distanceReturnsZeroForIdenticalPoints() {
        val latitude = 48.8566
        val longitude = 2.3522

        val distance = DistanceCalculator.distance(latitude, longitude, latitude, longitude)

        assertEquals(0.0, distance, 0.0)
    }

    @Test
    fun distanceMatchesExpectedValueForKnownCoordinates() {
        val parisLat = 48.8566
        val parisLon = 2.3522
        val londonLat = 51.5074
        val londonLon = -0.1278

        val distance = DistanceCalculator.distance(parisLat, parisLon, londonLat, londonLon)

        val expectedDistanceMeters = 343_556.0
        val toleranceMeters = 1_000.0

        assertEquals(expectedDistanceMeters, distance, toleranceMeters)
    }
}
