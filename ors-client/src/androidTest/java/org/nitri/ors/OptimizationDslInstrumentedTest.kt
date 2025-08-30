package org.nitri.ors

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.optimization.OptimizationRequestBuilderJ
import org.nitri.ors.domain.optimization.optimizationRequest

@RunWith(AndroidJUnit4::class)
class OptimizationDslInstrumentedTest {

    @Test
    fun dsl_buildsOptimizationRequest_andValidates() {
        val req = optimizationRequest {
            vehicle(id = 1, profile = "driving-car", startLon = 8.68, startLat = 49.41)
            job(id = 10, locationLon = 8.69, locationLat = 49.42, service = 300, priority = 50)
        }
        assertEquals(1, req.vehicles.size)
        assertEquals(1, req.jobs?.size)
    }

    @Test
    fun dsl_requires_vehicle_and_job_or_shipment() {
        assertThrows(IllegalStateException::class.java) {
            optimizationRequest { job(id = 10, locationLon = 8.0, locationLat = 48.0) }
        }
        assertThrows(IllegalStateException::class.java) {
            optimizationRequest { vehicle(id = 1, profile = "driving-car") }
        }
    }

    @Test
    fun javaBuilder_buildsOptimizationRequest_andValidates() {
        val req = OptimizationRequestBuilderJ()
            .addVehicle(1, "driving-car", 8.0, 48.0, null, null, null)
            .addJob(10, 8.1, 48.1, 120, 10)
            .build()
        assertEquals(1, req.vehicles.size)
        assertEquals(1, req.jobs?.size)
    }
}
