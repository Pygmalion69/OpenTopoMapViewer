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
import org.nitri.ors.helper.ElevationHelper

@RunWith(AndroidJUnit4::class)
class ElevationInstrumentedTest {

    private fun create(context: Context): Pair<DefaultOrsClient, ElevationHelper> {
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)
        val helper = ElevationHelper()
        return client to helper
    }

    @Test
    fun testElevation_point_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, helper) = create(context)

        // A point near Heidelberg, Germany
        val lon = 8.681495
        val lat = 49.41461

        val response = with(helper) { client.getElevationPoint(lon = lon, lat = lat) }

        assertNotNull("Elevation point response should not be null", response)
        assertNotNull("Geometry should not be null", response.geometry)
        assertEquals("Geometry type should be Point", "Point", response.geometry.type)

        val coords = response.geometry.coordinates
        assertTrue("Point coordinates should contain at least [lon, lat]", coords.size >= 2)
        // Typically API returns elevation as third value
        if (coords.size >= 3) {
            // elevation could be any double, just ensure it's a number
            val elevation = coords[2]
            assertTrue("Elevation should be a finite number", elevation.isFinite())
        }
    }

    @Test
    fun testElevation_line_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, helper) = create(context)

        // A short line segment around Heidelberg
        val coordinates = listOf(
            listOf(8.681495, 49.41461),
            listOf(8.687872, 49.420318)
        )

        val response = with(helper) { client.getElevationLine(coordinates = coordinates) }

        assertNotNull("Elevation line response should not be null", response)
        assertEquals("Geometry type should be LineString", "LineString", response.geometry.type)
        val lineCoords = response.geometry.coordinates
        assertTrue("LineString should have at least 2 points", lineCoords.size >= 2)

        val first = lineCoords.first()
        assertTrue("Each coordinate should have at least [lon, lat]", first.size >= 2)
        if (first.size >= 3) {
            val elevation = first[2]
            assertTrue("Elevation should be a finite number", elevation.isFinite())
        }
    }
}
