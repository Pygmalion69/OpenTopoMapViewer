package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.route.routeRequest

@RunWith(AndroidJUnit4::class)
class RouteDslInstrumentedTest {

    @Test
    fun buildRouteRequest_withDsl_buildsCoordinatesAndLanguage() {
        val req = routeRequest {
            language = "en"
            start(8.68, 49.41)
            coordinate(8.69, 49.42)
            end(8.70, 49.43)
        }
        assertEquals(3, req.coordinates.size)
        assertEquals(listOf(8.68, 49.41), req.coordinates.first())
        assertEquals(listOf(8.70, 49.43), req.coordinates.last())
        assertEquals("en", req.language)
    }

    @Test
    fun buildRouteRequest_withDsl_throwsIfLessThanTwoPoints() {
        assertThrows(IllegalArgumentException::class.java) {
            routeRequest {
                start(8.68, 49.41)
            }
        }
    }

    @Test
    fun dslIntegration_getRoute_succeedsBasic() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)

        val req = routeRequest {
            // Very short route to minimize load
            start(8.681495, 49.41461)
            end(8.686507, 49.41943)
            language = "en"
        }

        val response = client.getRoute(Profile.DRIVING_CAR, req)
        assertNotNull(response)
        // basic sanity: at least one route and segments present
        require(response.routes.isNotEmpty())
    }
}
