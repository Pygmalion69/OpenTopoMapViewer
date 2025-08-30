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
import org.nitri.ors.helper.MatrixHelper

@RunWith(AndroidJUnit4::class)
class MatrixInstrumentedTest {

    private fun create(context: Context): Pair<DefaultOrsClient, MatrixHelper> {
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)
        val helper = MatrixHelper()
        return client to helper
    }

    @Test
    fun testMatrix_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, helper) = create(context)

        // Two locations in/near Heidelberg, Germany [lon, lat]
        val locations = listOf(
            listOf(8.681495, 49.41461),   // Heidelberg center
            listOf(8.687872, 49.420318)   // Nearby point
        )
        val profile = Profile.DRIVING_CAR
        val metrics = listOf("duration", "distance")

        val response = with(helper) {
            client.getMatrix(
                locations = locations,
                profile = profile,
                metrics = metrics,
                resolveLocations = false
            )
        }

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