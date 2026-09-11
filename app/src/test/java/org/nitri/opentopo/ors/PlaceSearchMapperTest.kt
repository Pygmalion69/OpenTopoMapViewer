package org.nitri.opentopo.ors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.nitri.ors.domain.geocode.GeocodeFeature
import org.nitri.ors.domain.geocode.GeocodeGeometry
import org.nitri.ors.domain.geocode.GeocodeProperties

class PlaceSearchMapperTest {

    @Test
    fun validLonLat_mapsCorrectly() {
        val feature = GeocodeFeature(
            type = "Feature",
            geometry = GeocodeGeometry(type = "Point", coordinates = listOf(6.1234, 51.5678)),
            properties = GeocodeProperties(
                id = "id1",
                gid = "gid1",
                name = "Kleve",
                label = "Kleve, North Rhine-Westphalia, Germany"
            )
        )

        val result = mapGeocodeFeatureToPlaceSearchResult(feature)

        assertNotNull(result)
        assertEquals("gid1", result!!.stableId)
        assertEquals("Kleve", result.name)
        assertEquals("Kleve, North Rhine-Westphalia, Germany", result.label)
        assertEquals(6.1234, result.longitude, 0.00001)
        assertEquals(51.5678, result.latitude, 0.00001)
    }

    @Test
    fun nonPointGeometry_returnsNull() {
        val featureLine = GeocodeFeature(
            geometry = GeocodeGeometry(type = "LineString", coordinates = listOf(10.0, 20.0)),
            properties = GeocodeProperties(name = "Line", label = "Line Label")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(featureLine))

        val featurePolygon = GeocodeFeature(
            geometry = GeocodeGeometry(type = "Polygon", coordinates = listOf(10.0, 20.0)),
            properties = GeocodeProperties(name = "Poly", label = "Poly Label")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(featurePolygon))
    }

    @Test
    fun longitudeAndLatitude_areNotSwapped() {
        // Longitude = 12.34, Latitude = 56.78
        val feature = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(12.34, 56.78)),
            properties = GeocodeProperties(name = "Test", label = "Test Label")
        )

        val result = mapGeocodeFeatureToPlaceSearchResult(feature)!!
        assertEquals(12.34, result.longitude, 0.00001)
        assertEquals(56.78, result.latitude, 0.00001)
    }

    @Test
    fun labelAndName_fallbacks() {
        // Label missing -> use name
        val featureNoLabel = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(10.0, 20.0)),
            properties = GeocodeProperties(name = "Only Name", label = "")
        )
        val res1 = mapGeocodeFeatureToPlaceSearchResult(featureNoLabel)!!
        assertEquals("Only Name", res1.name)
        assertEquals("Only Name", res1.label)

        // Name missing -> use label
        val featureNoName = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(10.0, 20.0)),
            properties = GeocodeProperties(name = null, label = "Only Label")
        )
        val res2 = mapGeocodeFeatureToPlaceSearchResult(featureNoName)!!
        assertEquals("Only Label", res2.name)
        assertEquals("Only Label", res2.label)
    }

    @Test
    fun missingGeometry_returnsNull() {
        val feature = GeocodeFeature(
            geometry = null,
            properties = GeocodeProperties(name = "Name", label = "Label")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(feature))
    }

    @Test
    fun fewerThanTwoCoordinates_returnsNull() {
        val feature = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(10.0)),
            properties = GeocodeProperties(name = "Name", label = "Label")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(feature))
    }

    @Test
    fun nonFiniteCoordinates_returnsNull() {
        val featureNan = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(Double.NaN, 50.0)),
            properties = GeocodeProperties(name = "Name", label = "Label")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(featureNan))

        val featureInf = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(10.0, Double.POSITIVE_INFINITY)),
            properties = GeocodeProperties(name = "Name", label = "Label")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(featureInf))
    }

    @Test
    fun outOfRangeCoordinates_returnsNull() {
        val featureLonOutOfRange = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(181.0, 50.0)),
            properties = GeocodeProperties(name = "Name", label = "Label")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(featureLonOutOfRange))

        val featureLatOutOfRange = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(10.0, -91.0)),
            properties = GeocodeProperties(name = "Name", label = "Label")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(featureLatOutOfRange))
    }

    @Test
    fun missingPropertiesOrText_returnsNull() {
        val featureNoProps = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(10.0, 20.0)),
            properties = null
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(featureNoProps))

        val featureBlankText = GeocodeFeature(
            geometry = GeocodeGeometry(coordinates = listOf(10.0, 20.0)),
            properties = GeocodeProperties(name = "   ", label = "  ")
        )
        assertNull(mapGeocodeFeatureToPlaceSearchResult(featureBlankText))
    }

    @Test
    fun listMapping_skipsInvalidEntriesAndKeepsValid() {
        val features = listOf(
            GeocodeFeature(
                geometry = GeocodeGeometry(coordinates = listOf(10.0, 20.0)),
                properties = GeocodeProperties(name = "Valid 1", label = "Valid 1")
            ),
            GeocodeFeature(
                geometry = null,
                properties = GeocodeProperties(name = "Invalid", label = "Invalid")
            ),
            GeocodeFeature(
                geometry = GeocodeGeometry(coordinates = listOf(30.0, 40.0)),
                properties = GeocodeProperties(name = "Valid 2", label = "Valid 2")
            )
        )

        val results = mapGeocodeFeaturesToPlaceSearchResults(features)

        assertEquals(2, results.size)
        assertEquals("Valid 1", results[0].name)
        assertEquals("Valid 2", results[1].name)
    }
}
