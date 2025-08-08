package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.client.OpenRouteServiceClient
import org.nitri.ors.model.route.RouteRequest
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class RouteRepositoryGpxInstrumentedTest {

    @Test
    fun testFetchGpxRoute_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apiKey = context.getString(R.string.ors_api_key)
        val api = OpenRouteServiceClient.create(apiKey, context)

        val request = RouteRequest(
            coordinates = listOf(
                listOf(8.681495, 49.41461),
                listOf(8.687872, 49.420318)
            )
        )

        val response: Response<ResponseBody> = api.getRouteGpx("driving-car", request)

        val gpxXml = response.body()?.string()

        assertNotNull("GPX response body should not be null", gpxXml)
        assert(gpxXml!!.contains("<gpx")) { "Response does not appear to be valid GPX" }
    }
}
