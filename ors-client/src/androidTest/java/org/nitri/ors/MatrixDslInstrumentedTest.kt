package org.nitri.ors

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.matrix.MatrixRequestBuilderJ
import org.nitri.ors.domain.matrix.matrixRequest

@RunWith(AndroidJUnit4::class)
class MatrixDslInstrumentedTest {

    @Test
    fun dsl_buildsMatrixRequest_andValidates() {
        val req = matrixRequest {
            location(8.681495, 49.41461)
            location(8.686507, 49.41943)
            metrics = listOf("duration", "distance")
            resolveLocations = false
        }
        assertEquals(2, req.locations.size)
        assertEquals(listOf(8.681495,49.41461), req.locations.first())
        assertEquals(listOf("duration","distance"), req.metrics)
    }

    @Test
    fun dsl_requires_location() {
        assertThrows(IllegalArgumentException::class.java) {
            matrixRequest { /* no locations */ }
        }
    }

    @Test
    fun javaBuilder_buildsMatrixRequest_andValidates() {
        val req = MatrixRequestBuilderJ()
            .location(8.0, 48.0)
            .location(9.0, 49.0)
            .metrics(listOf("duration"))
            .resolveLocations(true)
            .build()
        assertEquals(2, req.locations.size)
        assertEquals(listOf(8.0,48.0), req.locations.first())
        assertEquals(listOf("duration"), req.metrics)
    }
}
