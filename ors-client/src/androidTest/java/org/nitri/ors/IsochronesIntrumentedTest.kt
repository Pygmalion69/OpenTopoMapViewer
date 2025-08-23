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
import org.nitri.ors.repository.IsochronesRepository

@RunWith(AndroidJUnit4::class)
class IsochronesInstrumentedTest {

    private fun createRepository(context: Context): IsochronesRepository {
        val apiKey = context.getString(R.string.ors_api_key)
        val api = OpenRouteServiceClient.create(apiKey, context)
        return IsochronesRepository(api)
    }

    @Test
    fun testIsochrones_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)

        // Heidelberg, Germany [lon, lat]
        val locations = listOf(
            listOf(8.681495, 49.41461)
        )
        // 5 minutes (300 seconds)
        val range = listOf(300)
        val profile = "driving-car"

        val response = repository.getIsochrones(
            locations = locations,
            range = range,
            profile = profile,
            attributes = null,
            rangeType = "time"
        )

        assertNotNull("Isochrones response should not be null", response)
        assertTrue("Features should not be empty", response.features.isNotEmpty())

        val first = response.features.first()
        assertNotNull("Feature geometry should not be null", first.geometry)

        // Basic metadata sanity
        assertTrue("Response type should not be blank", response.type.isNotBlank())
        assertNotNull("Metadata should be present", response.metadata)
        assertTrue("BBox should have 4 numbers", response.bbox.size == 4)
    }
}
