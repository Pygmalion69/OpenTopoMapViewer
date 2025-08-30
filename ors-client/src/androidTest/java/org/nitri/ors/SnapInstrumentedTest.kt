package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.helper.SnapHelper

@RunWith(AndroidJUnit4::class)
class SnapInstrumentedTest {

    private fun create(context: Context): Pair<DefaultOrsClient, SnapHelper> {
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)
        val helper = SnapHelper()
        return client to helper
    }

    @Test
    fun testSnap_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, helper) = create(context)

        // Two locations in/near Heidelberg, Germany [lon, lat]
        val locations = listOf(
            listOf(8.681495, 49.41461),   // Heidelberg center
            listOf(8.687872, 49.420318)   // Nearby point
        )
        val profile = Profile.DRIVING_CAR
        val radius = 50 // meters

        val response = with(helper) {
            client.getSnap(
                locations = locations,
                radius = radius,
                profile = profile,
                id = "snap_test"
            )
        }

        assertNotNull("Snap response should not be null", response)
        assertNotNull("Metadata should be present", response.metadata)
        assertTrue("Locations should not be empty", response.locations.isNotEmpty())
        assertEquals("Should have as many results as inputs", locations.size, response.locations.size)

        val first = response.locations.first()
        assertNotNull("First snapped location should have coordinates", first.location)
        assertEquals("Snapped coordinates should have 2 values [lon, lat]", 2, first.location.size)
        assertTrue("Snapped distance should be non-negative", first.snappedDistance >= 0.0)
    }

    @Test
    fun testSnapJson_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, helper) = create(context)

        val locations = listOf(
            listOf(8.681495, 49.41461),
            listOf(8.687872, 49.420318)
        )
        val profile = Profile.DRIVING_CAR
        val radius = 50

        val response = with(helper) {
            client.getSnapJson(
                locations = locations,
                radius = radius,
                profile = profile,
                id = "snap_json_test"
            )
        }

        assertNotNull("Snap JSON response should not be null", response)
        assertNotNull("Metadata should be present", response.metadata)
        assertTrue("Locations should not be empty", response.locations.isNotEmpty())
        response.locations.forEach { loc ->
            assertEquals("Coordinate should be [lon, lat]", 2, loc.location.size)
            assertTrue("Snapped distance should be non-negative", loc.snappedDistance >= 0.0)
        }
    }

    @Test
    fun testSnapGeoJson_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, repository) = create(context)

        val locations = listOf(
            listOf(8.681495, 49.41461),
            listOf(8.687872, 49.420318)
        )
        val profile = Profile.DRIVING_CAR
        val radius = 50

        val response = with(repository) {
            client.getSnapGeoJson(
                locations = locations,
                radius = radius,
                profile = profile,
                id = "snap_geojson_test"
            )
        }

        assertNotNull("Snap GeoJSON response should not be null", response)
        assertEquals("GeoJSON type should be FeatureCollection", "FeatureCollection", response.type)
        assertNotNull("Metadata should be present", response.metadata)
        assertTrue("Features should not be empty", response.features.isNotEmpty())

        val feature = response.features.first()
        assertEquals("Feature type should be Feature", "Feature", feature.type)
        assertEquals("Geometry type should be Point", "Point", feature.geometry.type)
        assertEquals("Point coordinates should be [lon, lat]", 2, feature.geometry.coordinates.size)
        assertTrue("snapped_distance should be non-negative", feature.properties.snappedDistance >= 0.0)
        assertTrue("source_id should be non-negative", feature.properties.sourceId >= 0)
    }
}
