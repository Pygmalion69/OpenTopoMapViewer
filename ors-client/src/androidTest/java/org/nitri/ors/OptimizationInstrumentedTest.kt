package org.nitri.ors

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.optimization.Job
import org.nitri.ors.domain.optimization.Vehicle
import org.nitri.ors.helper.OptimizationHelper

@RunWith(AndroidJUnit4::class)
class OptimizationInstrumentedTest {

    private fun create(context: Context): Pair<DefaultOrsClient, OptimizationHelper> {
        val apiKey = context.getString(R.string.ors_api_key)
        val client = DefaultOrsClient(apiKey, context)
        val helper = OptimizationHelper()
        return client to helper
    }

    @Test
    fun testOptimization_successful() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (client, helper) = create(context)

        // Simple scenario in/near Heidelberg, Germany
        val vehicle = Vehicle(
            id = 1,
            profile = "driving-car",
            // Start and end at/near Heidelberg castle parking
            start = listOf(8.6910, 49.4100),
            end = listOf(8.6910, 49.4100)
        )

        val jobs = listOf(
            Job(
                id = 101,
                location = listOf(8.681495, 49.41461) // Heidelberg center
            ),
            Job(
                id = 102,
                location = listOf(8.687872, 49.420318) // Nearby point
            )
        )

        val response = with(helper) {
            client.getOptimization(
                vehicles = listOf(vehicle),
                jobs = jobs
            )
        }

        // Basic assertions
        assertNotNull("Optimization response should not be null", response)
        assertNotNull("Summary should be present", response.summary)
        assertTrue("Routes list should be present", response.routes != null)
        assertTrue("Routes should not be empty for solvable small case", response.routes.isNotEmpty())

        val firstRoute = response.routes.first()
        assertTrue("Route steps should not be empty", firstRoute.steps.isNotEmpty())
        // Code is typically 0 for success in VROOM-like APIs; ensure non-negative as a safe check
        assertTrue("Response code should be non-negative", response.code >= 0)

        // Optional additional sanity checks
        assertTrue("Total duration should be >= 0", response.summary.duration >= 0)
        assertTrue("Total service should be >= 0", response.summary.service >= 0)
    }
}
