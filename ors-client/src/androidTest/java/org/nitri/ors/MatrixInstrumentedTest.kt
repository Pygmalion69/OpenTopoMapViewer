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
import org.nitri.ors.client.OpenRouteServiceClient
import org.nitri.ors.repository.MatrixRepository

@RunWith(AndroidJUnit4::class)
class MatrixInstrumentedTest {

    private fun createRepository(context: Context): MatrixRepository {
        val apiKey = context.getString(R.string.ors_api_key)
        val api = OpenRouteServiceClient.create(apiKey, context)
        return MatrixRepository(api)
    }

    @Test
    fun testMatrix_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)

        // Two locations in/near Heidelberg, Germany [lon, lat]
        val locations = listOf(
            listOf(8.681495, 49.41461),   // Heidelberg center
            listOf(8.687872, 49.420318)   // Nearby point
        )
        val profile = "driving-car"
        val metrics = listOf("duration", "distance")

        val response = repository.getMatrix(
            locations = locations,
            profile = profile,
            metrics = metrics,
            resolveLocations = false
        )

        assertNotNull("Matrix response should not be null", response)

        // Durations and distances should be present when requested
        val durations = response.durations
        val distances = response.distances
        assertNotNull("Durations should be present when requested", durations)
        assertNotNull("Distances should be present when requested", distances)

        // Expect a 2x2 matrix for two input points
        durations!!
        distances!!
        assertEquals("Durations should have size equal to number of sources", 2, durations.size)
        assertEquals("Distances should have size equal to number of sources", 2, distances.size)
        assertEquals("Each durations row should have size equal to number of destinations", 2, durations[0].size)
        assertEquals("Each distances row should have size equal to number of destinations", 2, distances[0].size)

        // Sanity: diagonal (origin to same point) should be zero or near-zero for distance
        assertTrue("Distance from a point to itself should be >= 0", distances[0][0] >= 0.0)
        assertTrue("Duration from a point to itself should be >= 0", durations[0][0] >= 0.0)

        // Metadata should be included
        assertNotNull("Metadata should be present", response.metadata)
    }
}