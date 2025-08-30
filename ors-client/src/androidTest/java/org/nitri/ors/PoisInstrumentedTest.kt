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
import org.nitri.ors.helper.PoisHelper

//@RunWith(AndroidJUnit4::class)
class PoisInstrumentedTest {

    private fun create(context: Context): Pair<DefaultOrsClient, PoisHelper> {
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)
        val helper = PoisHelper()
        return client to helper
    }

    // Leave it out for now: https://github.com/GIScience/openpoiservice/issues/150
//    @Test
//    fun testPois_byBbox_successful() = runBlocking {
//        val context = ApplicationProvider.getApplicationContext<Context>()
//        val (client, helper) = create(context)
//
//        // Bounding box around Heidelberg, Germany
//        val bbox = listOf(
//            listOf(8.67, 49.40), // minLon, minLat
//            listOf(8.70, 49.43)  // maxLon, maxLat
//        )
//
//        val response = with(helper) {
//            client.getPoisByBbox(
//                bbox = bbox,
//                limit = 10
//            )
//        }
//
//        assertNotNull("POIs response should not be null", response)
//        assertEquals("GeoJSON type should be FeatureCollection", "FeatureCollection", response.type)
//        assertNotNull("Information block should be present", response.information)
//        assertTrue("Features should not be empty in a city bbox", response.features.isNotEmpty())
//
//        val first = response.features.first()
//        assertEquals("Feature type should be Feature", "Feature", first.type)
//        assertEquals("Geometry type should be Point", "Point", first.geometry.type)
//        assertEquals("Point coordinates should be [lon, lat]", 2, first.geometry.coordinates.size)
//
//        // Basic properties sanity
//        val props = first.properties
//        assertTrue("OSM id should be positive", props.osmId > 0)
//        assertTrue("Distance should be non-negative", props.distance >= 0.0)
//    }
}
