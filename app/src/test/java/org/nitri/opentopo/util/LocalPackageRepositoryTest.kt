package org.nitri.opentopo.util

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class LocalPackageRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun png(color: Int = 0xff883311.toInt(), size: Int = 256): ByteArray {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, color)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
                }
            }
        }.toByteArray()
    private fun repository(root: File, archiveLimit: Long = LocalPackageLimits.ARCHIVE_BYTES,
                           totalLimit: Long = LocalPackageLimits.TOTAL_BYTES) =
        LocalPackageRepository(root, { bytes ->
            val image = ImageIO.read(ByteArrayInputStream(bytes))
            require(image != null && image.width == 256 && image.height == 256)
            image.flush()
        }, archiveLimit, totalLimit)

    @Test fun overlappingPackagesSurviveReopenAndSourceDeletionAndRemoveOnlyById() = runTest {
        val root = temporary.newFolder()
        val repo = repository(root)
        val first = png()
        val second = png(0xff112233.toInt())
        val source = temporary.newFile("source.zip")
        source.writeBytes(zip("OpenTopoMap/15/17846/12351.png" to first,
            "OpenTopoMap/18/142768/98808.png" to first, "attribution.txt" to "© Example".toByteArray()))
        val a = repo.import("same.zip", { source.inputStream() })
        val b = repo.import("same.zip", { ByteArrayInputStream(zip("OpenTopoMap/15/17846/12351.png" to second)) })
        assertNotEquals(a.id, b.id)
        assertEquals(listOf(15, 18), a.zoomLevels)
        assertEquals(2, a.tileCount)
        assertEquals("© Example", a.attribution)
        assertTrue(a.west < a.east && a.south < a.north)
        source.delete()
        val restarted = repository(root)
        assertEquals(2, restarted.list().size)
        val ra = restarted.open(a.id)!!
        val rb = restarted.open(b.id)!!
        assertEquals(a.id, ra.sourceId)
        assertArrayEquals(first, ra.read(15, 17846, 12351))
        assertArrayEquals(second, rb.read(15, 17846, 12351))
        restarted.remove(a.id)
        assertNull(ra.read(15, 17846, 12351)) // removal closes an outstanding reader
        assertArrayEquals(second, rb.read(15, 17846, 12351))
        assertNull(restarted.lookup(a.id))
        assertEquals(listOf(b), restarted.list())
        rb.close()
    }

    @Test fun invalidPackagesNeverPublish() = runTest {
        val root = temporary.newFolder()
        val repo = repository(root)
        val tile = png()
        val cases = listOf(
            zip("README.txt" to byteArrayOf(1)),
            zip("../2/1/1.png" to tile),
            zip("/2/1/1.png" to tile),
            zip("root/../2/1/1.png" to tile),
            zip("root/30/1/1.png" to tile),
            zip("root/2/4/1.png" to tile),
            zip("root/2/-1/1.png" to tile),
            zip("a/2/1/1.png" to tile, "b/2/1/1.png" to tile),
            zip("2/1/1.png" to tile, "02/1/1.png" to tile),
            zip("2/1/1.png" to byteArrayOf(1, 2, 3)),
            zip("2/1/1.png" to png(size = 128)),
            zip("2/1/1.png" to tile.copyOf(tile.size - 6)),
            zip("2/1/1.png" to tile.copyOf().also { it[50] = (it[50].toInt() xor 1).toByte() }),
            byteArrayOf(1, 2, 3)
        )
        for (bytes in cases) {
            try { repo.import("bad", { ByteArrayInputStream(bytes) }); fail("Accepted invalid ZIP") }
            catch (_: InvalidLocalPackage) { }
            assertTrue(repo.list().isEmpty())
            assertTrue(File(root, "staging").listFiles()!!.isEmpty())
        }
    }

    @Test fun limitsReadFailuresAndCancellationCleanStaging() = runTest {
        val bytes = zip("2/1/1.png" to png())
        val root = temporary.newFolder()
        suspend fun assertEmpty() {
            assertTrue(repository(root).list().isEmpty())
            assertTrue(File(root, "staging").listFiles()!!.isEmpty())
        }
        try {
            repository(root, archiveLimit = 10).import("large", { ByteArrayInputStream(bytes) })
            fail()
        } catch (_: IllegalArgumentException) { }
        assertEmpty()
        try {
            repository(root, totalLimit = 1).import("inflated", { ByteArrayInputStream(bytes) })
            fail()
        } catch (_: InvalidLocalPackage) { }
        assertEmpty()
        try {
            repository(root).import("unreadable", { throw IOException("Unavailable") })
            fail()
        } catch (_: IOException) { }
        assertEmpty()
        for (duringValidation in listOf(false, true)) {
            val job = launch(Dispatchers.Default) {
                val context = currentCoroutineContext()
                repository(root).import("cancelled", { ByteArrayInputStream(bytes) }) { validating, _, _ ->
                    if (validating == duringValidation) context.cancel()
                }
            }
            job.join()
            assertTrue(job.isCancelled)
            assertEmpty()
        }
    }

    @Test fun abandonedStagingIsRecoveredWithoutRemovingInstalledPackages() = runTest {
        val root = temporary.newFolder()
        val repo = repository(root)
        val p = repo.import("kept", { ByteArrayInputStream(zip(
            "2/" to byteArrayOf(), "2/1/1.png" to png(), "README.txt" to "XYZ".toByteArray())) })
        File(root, "staging/dead/tiles.zip").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1)) }
        assertEquals(listOf(p), repository(root).list())
        assertTrue(File(root, "staging").listFiles()!!.isEmpty())
        repository(root).open(p.id)!!.use { assertNotNull(it.read(2, 1, 1)) }
        // A write failure before staging creation must not disturb installed data.
        val blocked = temporary.newFile("not-a-directory")
        try { repository(blocked).import("fail", { ByteArrayInputStream(byteArrayOf()) }); fail() }
        catch (_: IllegalStateException) { }
        assertEquals(p, repo.lookup(p.id))
    }

    @Test fun recoveryWaitsForLiveImport() = runTest {
        val root = temporary.newFolder()
        val started = CompletableDeferred<Unit>()
        val release = java.util.concurrent.CountDownLatch(1)
        val importing = async(Dispatchers.Default) {
            repository(root).import("live", {
                started.complete(Unit)
                release.await()
                ByteArrayInputStream(zip("2/1/1.png" to png()))
            })
        }
        started.await()
        val listing = async(Dispatchers.Default) { repository(root).list() }
        try {
            assertTrue(File(root, "staging").listFiles()!!.isNotEmpty())
        } finally { release.countDown() }
        val installed = importing.await()
        assertEquals(listOf(installed), listing.await())
    }

    @Test fun enforcesEntryAndActualReadLimitsAndCleansWriteFailure() = runTest {
        val root = temporary.newFolder()
        val repo = repository(root)
        val tile = png()
        val bytes = zip("2/1/1.png" to tile)
        val p = repo.import("kept", { ByteArrayInputStream(bytes) })
        // Fail metadata creation, after the ZIP was copied and validated.
        try {
            repo.import("write-failure", { ByteArrayInputStream(bytes) }) { validating, _, _ ->
                if (validating) {
                    val active = File(root, "staging").listFiles()!!.single()
                    File(active, "metadata.properties").mkdir()
                }
            }
            fail()
        } catch (_: IOException) { }
        assertEquals(listOf(p), repo.list())
        assertTrue(File(root, "staging").listFiles()!!.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            ByteArrayInputStream(ByteArray(11)).readNBytesCompat(10)
        }
        val tooMany = temporary.newFile("many.zip")
        tooMany.writeBytes(zip("2/1/1.png" to tile, "README" to byteArrayOf()))
        java.util.zip.ZipFile(tooMany).use { archive ->
            assertThrows(IllegalArgumentException::class.java) { XyzZipIndex.read(archive, 1) }
        }
        val metadataBomb = zip("2/1/1.png" to tile,
            "README" to ByteArray(LocalPackageLimits.METADATA_BYTES + 1))
        val tileBomb = zip("2/1/1.png" to ByteArray(LocalPackageLimits.TILE_BYTES + 1))
        for (bad in listOf(metadataBomb, tileBomb)) {
            try { repo.import("bomb", { ByteArrayInputStream(bad) }); fail() }
            catch (_: InvalidLocalPackage) { }
        }
        assertEquals(listOf(p), repo.list())
    }

    @Test fun validatesRealSampleWhenExplicitlyProvided() = runTest {
        val path = System.getenv("LOCAL_XYZ_SAMPLE") ?: return@runTest
        val source = File(path)
        val repo = repository(temporary.newFolder())
        val p = repo.import("Caperrino", { source.inputStream() })
        assertEquals(1692, p.tileCount)
        assertEquals(listOf(15, 16, 17, 18), p.zoomLevels)
        repo.open(p.id)!!.use { assertEquals(1692, it.tileCount) }
    }
}
