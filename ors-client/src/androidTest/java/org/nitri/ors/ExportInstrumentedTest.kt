package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.helper.ExportHelper

@RunWith(AndroidJUnit4::class)
class ExportInstrumentedTest {

    private fun create(context: Context): Pair<DefaultOrsClient, ExportHelper>  {
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)
        val helper = ExportHelper()
        return client to helper
    }

    @Test
    fun testExport_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, helper) = create(context)

        // Bounding box around Heidelberg, Germany
        val bbox = listOf(
            listOf(8.681495, 49.41461),   // minLon, minLat
            listOf(8.686507, 49.41943)    // maxLon, maxLat
        )

        val response = with(helper) { client.export(bbox = bbox, profile = Profile.DRIVING_CAR) }

        assertNotNull("Export response should not be null", response)
        // Basic sanity checks on structure
        assertTrue("Points list should not be empty", response.nodes.isNotEmpty())
        assertTrue("Edges list should not be empty", response.edges.isNotEmpty())
    }

    // Exceeds rate limit
//    @Test
//    fun testExportJson_successful() = runBlocking {
//        val context = ApplicationProvider.getApplicationContext<Context>()
//
//        val (client, helper) = create(context)
//        val bbox = listOf(
//            listOf(8.681495, 49.41461),
//            listOf(8.686507, 49.41943)
//        )
//
//        val response = with(helper) { client.exportJson(bbox = bbox, profile = "driving-car") }
//
//        assertNotNull("JSON Export response should not be null", response)
//        assertTrue("Points list should not be empty", response.nodes.isNotEmpty())
//    }

    // Exceeds rate limit
//    @Test
//    fun testExportTopoJson_successful() = runBlocking {
//        val context = ApplicationProvider.getApplicationContext<Context>()
//        val (client, helper) = create(context)
//
//        val bbox = listOf(
//            listOf(8.681495, 49.41461),
//            listOf(8.686507, 49.41943)
//        )
//
//        val topo = with(helper) { client.exportTopoJson(bbox = bbox, profile = "driving-car") }
//
//        assertNotNull("TopoJSON Export response should not be null", topo)
//        assertTrue("Type should be present", topo.type.isNotBlank())
//    }
}
