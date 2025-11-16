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
        instantiateViaConstructors(latitude, longitude)?.let { return it }
        instantiateViaUnsafe(latitude, longitude)?.let { return it }

        val constructors = Point::class.java.declaredConstructors
        error("Unable to instantiate Point via reflection. Constructors: ${constructors.joinToString { it.toGenericString() }}")
    }

    private fun instantiateViaConstructors(latitude: Double, longitude: Double): Point? {
        val constructors = Point::class.java.declaredConstructors

        for (constructor in constructors) {
            constructor.isAccessible = true
            val parameterTypes = constructor.parameterTypes

            var latAssigned = false
            var lonAssigned = false

            val arguments = Array<Any?>(parameterTypes.size) { index ->
                val parameterType = parameterTypes[index]
                when {
                    parameterType.isLatitudeLongitudeParameter() && !latAssigned -> {
                        latAssigned = true
                        latitude
                    }
                    parameterType.isLatitudeLongitudeParameter() && !lonAssigned -> {
                        lonAssigned = true
                        longitude
                    }
                    parameterType.isPointBuilder() -> parameterType.instantiateBuilder(latitude, longitude)
                    else -> parameterType.defaultValue()
                }
            }

            val hasAllCoordinates = (latAssigned && lonAssigned) ||
                arguments.any { argument -> argument?.isBuilderWithCoordinates() == true }

            if (hasAllCoordinates) {
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

        return null
    }

    private fun instantiateViaUnsafe(latitude: Double, longitude: Double): Point? {
        return try {
            val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
            unsafeField.isAccessible = true
            val unsafe = unsafeField.get(null) as sun.misc.Unsafe
            val instance = unsafe.allocateInstance(Point::class.java) as Point
            val latitudeAssigned = instance.assignCoordinate(latitude, listOf("lat", "latitude"))
            val longitudeAssigned = instance.assignCoordinate(longitude, listOf("lon", "lng", "long", "longitude"))
            if (latitudeAssigned && longitudeAssigned) {
                instance
            } else {
                null
            }
        } catch (ignored: ReflectiveOperationException) {
            null
        }
    }

    private fun Class<*>.isLatitudeLongitudeParameter(): Boolean {
        return this == java.lang.Double.TYPE ||
            this == java.lang.Double::class.java ||
            this == java.lang.Float.TYPE ||
            this == java.lang.Float::class.java
    }

    private fun Class<*>.isPointBuilder(): Boolean {
        return enclosingClass == Point::class.java && simpleName.contains("Builder", ignoreCase = true)
    }

    private fun Class<*>.instantiateBuilder(latitude: Double, longitude: Double): Any? {
        for (constructor in declaredConstructors.sortedBy { it.parameterCount }) {
            constructor.isAccessible = true
            val args = Array(constructor.parameterCount) { index ->
                constructor.parameterTypes[index].defaultValue()
            }

            try {
                val builder = constructor.newInstance(*args)
                val latAssigned = builder.assignCoordinate(latitude, listOf("lat", "latitude"))
                val lonAssigned = builder.assignCoordinate(longitude, listOf("lon", "lng", "long", "longitude"))
                if (latAssigned && lonAssigned) {
                    return builder
                }
            } catch (ignored: ReflectiveOperationException) {
                // try next constructor
            }
        }

        return null
    }

    private fun Any.assignCoordinate(value: Double, keywords: List<String>): Boolean {
        val targetClass = this::class.java
        val setter = targetClass.declaredMethods.firstOrNull { method ->
            method.parameterTypes.size == 1 &&
                method.parameterTypes[0].isLatitudeLongitudeParameter() &&
                keywords.any { keyword -> method.name.contains(keyword, ignoreCase = true) }
        }

        if (setter != null) {
            setter.isAccessible = true
            val parameterType = setter.parameterTypes[0]
            setter.invoke(this, convertCoordinate(value, parameterType))
            return true
        }

        val field = targetClass.declaredFields.firstOrNull { field ->
            field.type.isLatitudeLongitudeParameter() &&
                keywords.any { keyword -> field.name.contains(keyword, ignoreCase = true) }
        }

        if (field != null) {
            field.isAccessible = true
            when (field.type) {
                java.lang.Double.TYPE, java.lang.Double::class.java -> field.setDouble(this, value)
                java.lang.Float.TYPE, java.lang.Float::class.java -> field.setFloat(this, value.toFloat())
                else -> field.set(this, convertCoordinate(value, field.type))
            }
            return true
        }

        return false
    }

    private fun Any.isBuilderWithCoordinates(): Boolean {
        val clazz = this::class.java
        val hasLat = clazz.declaredFields.any { field ->
            field.type.isLatitudeLongitudeParameter() && field.name.contains("lat", ignoreCase = true)
        }
        val hasLon = clazz.declaredFields.any { field ->
            field.type.isLatitudeLongitudeParameter() &&
                (field.name.contains("lon", ignoreCase = true) || field.name.contains("long", ignoreCase = true))
        }
        return hasLat && hasLon
    }

    private fun convertCoordinate(value: Double, parameterType: Class<*>): Any = when (parameterType) {
        java.lang.Float.TYPE, java.lang.Float::class.java -> value.toFloat()
        else -> value
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
