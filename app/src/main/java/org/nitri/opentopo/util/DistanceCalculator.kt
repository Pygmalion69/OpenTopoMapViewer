package org.nitri.opentopo.util

import io.ticofab.androidgpxparser.parser.domain.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceCalculator {

    /**
     * Calculates the distance between two geographical points in meters.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return Distance in meters.
     */
    fun distance(point1: Point, point2: Point): Double {
        return distance(point1.latitude, point1.longitude, point2.latitude, point2.longitude)
    }

    /**
     * Calculates the distance between two latitude/longitude pairs in meters.
     *
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return Distance in meters.
     */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == lat2 && lon1 == lon2) return 0.0

        val earthRadius = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}