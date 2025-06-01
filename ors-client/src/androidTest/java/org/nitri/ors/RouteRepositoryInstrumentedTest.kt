package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.client.OpenRouteServiceClient
import org.nitri.ors.repository.RouteRepository

@RunWith(AndroidJUnit4::class)
class RouteRepositoryInstrumentedTest {

    @Test
    fun testFetchRoute_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apiKey = context.getString(R.string.ors_api_key)
        val client = OpenRouteServiceClient.create(apiKey)
        val repository = RouteRepository(client)

        val start = Pair(8.681495, 49.41461)
        val end = Pair(8.687872, 49.420318)

        val route = repository.getRoute(start, end, "driving-car")

        assertNotNull("Route should not be null", route)
    }
}
