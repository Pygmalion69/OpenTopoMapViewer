package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.client.OpenRouteServiceClient
import org.nitri.ors.repository.GeocodeRepository

@RunWith(AndroidJUnit4::class)
class GeocodeInstrumentedTest {

    private fun createRepository(context: Context): GeocodeRepository {
        val apiKey = context.getString(R.string.ors_api_key)
        val api = OpenRouteServiceClient.create(apiKey, context)
        return GeocodeRepository(api)
    }

    @Test
    fun testGeocode_search_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = createRepository(context)
        val apiKey = context.getString(R.string.ors_api_key)

        val response = repo.search(
            text = "Heidelberg",
            apiKey = apiKey,
            size = 5
        )

        assertNotNull("Search response should not be null", response)
        assertTrue("Features should not be empty for Heidelberg search", response.features.isNotEmpty())

        val first = response.features.first()
        // Basic geometry sanity
        assertNotNull("First feature geometry should not be null", first.geometry)
        val geom = first.geometry!!
        assertTrue("Geometry coordinates should have at least [lon, lat]", geom.coordinates.size >= 2)
        // Basic properties sanity
        assertNotNull("First feature properties should not be null", first.properties)
        val name = first.properties?.name ?: first.properties?.label
        assertTrue("Feature should have a name or label", !name.isNullOrBlank())
    }

    @Test
    fun testGeocode_reverse_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = createRepository(context)
        val apiKey = context.getString(R.string.ors_api_key)

        // Point near Heidelberg, Germany
        val lon = 8.681495
        val lat = 49.41461

        val response = repo.reverse(
            apiKey = apiKey,
            lon = lon,
            lat = lat,
            size = 5
        )

        assertNotNull("Reverse response should not be null", response)
        assertTrue("Reverse should return at least one feature", response.features.isNotEmpty())
        val first = response.features.first()
        assertNotNull("First reverse feature properties should not be null", first.properties)
        val label = first.properties?.label ?: first.properties?.name
        assertTrue("Reverse feature should provide a label/name", !label.isNullOrBlank())
    }

    @Test
    fun testGeocode_autocomplete_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = createRepository(context)
        val apiKey = context.getString(R.string.ors_api_key)

        val response = repo.autocomplete(
            apiKey = apiKey,
            text = "Heidelb",
            size = 5
        )

        assertNotNull("Autocomplete response should not be null", response)
        assertTrue("Autocomplete should return suggestions", response.features.isNotEmpty())
    }
}
