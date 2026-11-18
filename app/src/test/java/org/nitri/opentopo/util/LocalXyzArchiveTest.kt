package org.nitri.opentopo.util

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LocalXyzArchiveTest {
    @Test fun indexesActualZoomsAndReturnsOnlyExactTiles() {
        val file = File.createTempFile("local-xyz", ".zip")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                listOf("OpenTopoMap/15/1/2.png", "OpenTopoMap/18/8/9.png").forEach {
                    zip.putNextEntry(ZipEntry(it))
                    zip.write(byteArrayOf(1, 2, 3))
                    zip.closeEntry()
                }
            }
            val archive = LocalXyzArchive(file)
            assertEquals(15, archive.minZoom)
            assertEquals(18, archive.maxZoom)
            assertEquals(2, archive.tileCount)
            assertArrayEquals(byteArrayOf(1, 2, 3), archive.read(18, 8, 9))
            assertNull(archive.read(18, 8, 10))
            assertNull(archive.read(14, 1, 2))
            archive.close()
            assertNull(archive.read(18, 8, 9))
            // Reopening the persistent package needs no conversion or cache state.
            LocalXyzArchive(file).use { assertNotNull(it.read(15, 1, 2)) }
        } finally {
            file.delete()
        }
    }

    @Test fun acceptsBarePathsAndOtherSingleRoots() {
        listOf("2/1/1.png", "Antonio/2/1/1.png").forEach { path ->
            val file = File.createTempFile("local-xyz", ".zip")
            try {
                ZipOutputStream(file.outputStream()).use {
                    it.putNextEntry(ZipEntry(path)); it.write(byteArrayOf(7)); it.closeEntry()
                }
                LocalXyzArchive(file, "stable-id").use {
                    assertEquals("stable-id", it.sourceId)
                    assertArrayEquals(byteArrayOf(7), it.read(2, 1, 1))
                }
            } finally { file.delete() }
        }
    }

    @Test fun rejectsPackagesWithoutValidXyzTiles() {
        val file = File.createTempFile("invalid-xyz", ".zip")
        try {
            ZipOutputStream(file.outputStream()).use { it.putNextEntry(ZipEntry("../18/1/1.png")) }
            assertThrows(IllegalArgumentException::class.java) { LocalXyzArchive(file) }
        } finally {
            file.delete()
        }
    }
}
