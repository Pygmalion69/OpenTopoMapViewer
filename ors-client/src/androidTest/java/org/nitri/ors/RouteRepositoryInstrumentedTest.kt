package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.client.OpenRouteServiceClient
import org.nitri.ors.model.route.GeoJsonRouteResponse
import org.nitri.ors.repository.ExportRepository
import org.nitri.ors.repository.RouteRepository
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class RouteRepositoryInstrumentedTest {

    private fun createRepository(context: Context): RouteRepository {
        val apiKey = context.getString(R.string.ors_api_key)
        val api = OpenRouteServiceClient.create(apiKey, context)
        return RouteRepository(api)
    }

    @Test
    fun testFetchRoute_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)

        val start = Pair(8.681495, 49.41461)
        val end = Pair(8.687872, 49.420318)

        val route = repository.getRoute(start, end, "driving-car")

        assertNotNull("Route should not be null", route)
    }

    @Test
    fun testFetchGpxRoute_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)

        val start = Pair(8.681495, 49.41461)
        val end = Pair(8.687872, 49.420318)

        val response: Response<ResponseBody> = repository.getRouteGpx(start, end,"driving-car")

        val gpxXml = response.body()?.string()

        assertNotNull("GPX response body should not be null", gpxXml)
        assert(gpxXml!!.contains("<gpx")) { "Response does not appear to be valid GPX" }
    }

    @Test
    fun testFetchGeoJsonRoute_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)

        val start = Pair(8.681495, 49.41461)
        val end = Pair(8.687872, 49.420318)

        val route: GeoJsonRouteResponse = repository.getRouteGeoJson(start, end, "driving-car")

        assertNotNull("Route should not be null", route)
    }
}
