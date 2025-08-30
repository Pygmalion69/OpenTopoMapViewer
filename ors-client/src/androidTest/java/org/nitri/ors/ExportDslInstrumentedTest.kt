package org.nitri.ors

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.export.ExportRequestBuilderJ
import org.nitri.ors.domain.export.exportRequest

@RunWith(AndroidJUnit4::class)
class ExportDslInstrumentedTest {

    @Test
    fun dsl_buildsExportRequest_andValidates() {
        val req = exportRequest {
            bbox(8.681495, 49.41461, 8.686507, 49.41943)
            id("test-id")
            geometry(true)
        }
        assertEquals("test-id", req.id)
        assertEquals(listOf(listOf(8.681495,49.41461), listOf(8.686507,49.41943)), req.bbox)
    }

    @Test
    fun dsl_requires_bbox() {
        assertThrows(IllegalArgumentException::class.java) {
            exportRequest { geometry(false) }
        }
    }

    @Test
    fun javaBuilder_buildsExportRequest_andValidates() {
        val req = ExportRequestBuilderJ()
            .bbox(8.0, 48.0, 9.0, 49.0)
            .id("jid")
            .geometry(null)
            .build()
        assertEquals("jid", req.id)
        assertEquals(listOf(listOf(8.0,48.0), listOf(9.0,49.0)), req.bbox)
    }
}
