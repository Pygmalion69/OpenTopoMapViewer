package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.route.GeoJsonRouteResponse
import org.nitri.ors.helper.RouteHelper

@RunWith(AndroidJUnit4::class)
class RouteInstrumentedTest {

    private fun create(context: Context): Pair<DefaultOrsClient, RouteHelper> {
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)
        val repo = RouteHelper()
        return client to repo
    }

    @Test
    fun testFetchRoute_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, repository) = create(context)

        val start = Pair(8.681495, 49.41461)
        val end = Pair(8.687872, 49.420318)

        val route = with(repository) { client.getRoute(start, end, "driving-car") }

        assertNotNull("Route should not be null", route)
    }

    @Test
    fun testFetchGpxRoute_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, repository) = create(context)

        val start = Pair(8.681495, 49.41461)
        val end = Pair(8.687872, 49.420318)

        val gpxXml = with(repository) { client.getRouteGpx(start, end, "driving-car") }

        assertNotNull("GPX response body should not be null", gpxXml)
        assert(gpxXml.contains("<gpx")) { "Response does not appear to be valid GPX" }
    }

    @Test
    fun testFetchGeoJsonRoute_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, repository) = create(context)

        val start = Pair(8.681495, 49.41461)
        val end = Pair(8.687872, 49.420318)

        val route: GeoJsonRouteResponse = with(repository) { client.getRouteGeoJson(start, end,
            Profile.DRIVING_CAR) }

        assertNotNull("Route should not be null", route)
    }
}
