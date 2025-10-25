package org.nitri.opentopo

import io.ticofab.androidgpxparser.parser.domain.Point
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

    @Test
    fun distanceBetweenPointsHandlesAntipodalLocations() {
        val pointNearGreenwich = createPoint(0.0, 0.0)
        val antipodalPoint = createPoint(0.0, 180.0)

        val distance = DistanceCalculator.distance(pointNearGreenwich, antipodalPoint)

        val expectedDistanceMeters = Math.PI * 6_371_000.0
        val toleranceMeters = 10_000.0

        assertEquals(expectedDistanceMeters, distance, toleranceMeters)
    }

    private fun createPoint(latitude: Double, longitude: Double): Point {
        val constructors = Point::class.java.declaredConstructors

        for (constructor in constructors) {
            constructor.isAccessible = true
            val parameterTypes = constructor.parameterTypes

            var latAssigned = false
            var lonAssigned = false

            val arguments = Array<Any?>(parameterTypes.size) { index ->
                val parameterType = parameterTypes[index]
                when {
                    !latAssigned && parameterType.isLatitudeLongitudeParameter() -> {
                        latAssigned = true
                        latitude
                    }
                    !lonAssigned && parameterType.isLatitudeLongitudeParameter() -> {
                        lonAssigned = true
                        longitude
                    }
                    else -> parameterType.defaultValue()
                }
            }

            if (latAssigned && lonAssigned) {
                try {
                    val instance = constructor.newInstance(*arguments)
                    if (instance is Point && instance.latitude == latitude && instance.longitude == longitude) {
                        return instance
                    }
                } catch (ignored: ReflectiveOperationException) {
                    // Try the next constructor
                }
            }
        }

        error("Unable to instantiate Point via reflection. Constructors: ${constructors.joinToString { it.toGenericString() }}")
    }

    private fun Class<*>.isLatitudeLongitudeParameter(): Boolean {
        return this == java.lang.Double.TYPE || this == java.lang.Double::class.java
    }

    private fun Class<*>.defaultValue(): Any? = when (this) {
        java.lang.Double.TYPE -> 0.0
        java.lang.Float.TYPE -> 0f
        java.lang.Long.TYPE -> 0L
        java.lang.Integer.TYPE -> 0
        java.lang.Boolean.TYPE -> false
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Character.TYPE -> 0.toChar()
        java.lang.String::class.java -> ""
        java.util.Date::class.java -> java.util.Date(0)
        java.math.BigDecimal::class.java -> java.math.BigDecimal.ZERO
        java.lang.Double::class.java -> null
        java.lang.Float::class.java -> null
        java.lang.Long::class.java -> null
        java.lang.Integer::class.java -> null
        java.lang.Boolean::class.java -> null
        java.lang.Short::class.java -> null
        java.lang.Byte::class.java -> null
        java.lang.Character::class.java -> null
        else -> when (this.name) {
            "kotlin.jvm.internal.DefaultConstructorMarker" -> null
            else -> null
        }
    }
}
