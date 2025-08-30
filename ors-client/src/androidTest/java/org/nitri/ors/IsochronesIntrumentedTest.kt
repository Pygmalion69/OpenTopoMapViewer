package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.helper.IsochronesHelper

@RunWith(AndroidJUnit4::class)
class IsochronesInstrumentedTest {

    private fun create(context: Context): Pair<DefaultOrsClient, IsochronesHelper> {
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)
        val helper = IsochronesHelper()
        return client to helper
    }

    @Test
    fun testIsochrones_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, helper) = create(context)

        // Heidelberg, Germany [lon, lat]
        val locations = listOf(
            listOf(8.681495, 49.41461)
        )
        // 5 minutes (300 seconds)
        val range = listOf(300)
        val profile = Profile.DRIVING_CAR

        val response = with(helper) {
            client.getIsochrones(
                locations = locations,
                range = range,
                profile = profile,
                attributes = null,
                rangeType = "time"
            )
        }

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
