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
import org.nitri.ors.domain.route.RouteRequestBuilderJ

@RunWith(AndroidJUnit4::class)
class RouteRequestBuilderJInstrumentedTest {

    @Test
    fun builder_buildsCoordinatesAndLanguage() {
        val req = RouteRequestBuilderJ()
            .start(8.68, 49.41)
            .add(8.69, 49.42)
            .end(8.70, 49.43)
            .language("de")
            .build()

        assertEquals(3, req.coordinates.size)
        assertEquals(listOf(8.68, 49.41), req.coordinates.first())
        assertEquals(listOf(8.70, 49.43), req.coordinates.last())
        assertEquals("de", req.language)
    }

    @Test
    fun builder_throwsIfLessThanTwoPoints() {
        assertThrows(IllegalArgumentException::class.java) {
            RouteRequestBuilderJ()
                .start(8.68, 49.41)
                .build()
        }
    }

    @Test
    fun builderIntegration_getRoute_succeedsBasic() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)

        val req = RouteRequestBuilderJ()
            .start(8.681495, 49.41461)
            .end(8.686507, 49.41943)
            .language("en")
            .build()

        val response = client.getRoute(Profile.DRIVING_CAR, req)
        assertNotNull(response)
        require(response.routes.isNotEmpty())
    }
}
