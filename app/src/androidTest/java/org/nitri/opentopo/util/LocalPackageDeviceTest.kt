package org.nitri.opentopo.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Target explicitly with the runner class argument; never changes the normal map cache.
 * On a device with user data, install the test APK with adb and run am instrument directly.
 * Gradle connectedAndroidTest cleanup may uninstall the target app and erase private data.
 */
class LocalPackageDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun repository(root: File) = LocalPackageRepository(root, { bytes ->
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        try { require(bitmap.width == 256 && bitmap.height == 256) } finally { bitmap.recycle() }
    })

    @Test fun pickerImportedSampleStillOpensById() = runBlocking {
        val id = InstrumentationRegistry.getArguments().getString("localPackageId")
        assumeNotNull(id) // Optional manual fixture; samples never enter source control.
        val repo = repository(File(context.noBackupFilesDir, "local-xyz"))
        val p = requireNotNull(repo.lookup(requireNotNull(id)))
        assertEquals(1692, p.tileCount)
        assertEquals(listOf(15, 16, 17, 18), p.zoomLevels)
        assertTrue(repo.list().any { it.id == id })
        repo.open(id)!!.use { reader ->
            val bytes = requireNotNull(reader.read(15, 17846, 12351))
            val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            try { assertEquals(256, bitmap.width); assertEquals(256, bitmap.height) }
            finally { bitmap.recycle() }
        }
    }

    @Test fun platformValidationAndIndependentReadsAndRemoval() = runBlocking {
        val root = File(context.noBackupFilesDir, "local-xyz-test-${UUID.randomUUID()}")
        try {
            fun zip(color: Int): ByteArray {
                val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                val png = ByteArrayOutputStream()
                try { bitmap.eraseColor(color); assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, png)) }
                finally { bitmap.recycle() }
                return ByteArrayOutputStream().also { out ->
                    ZipOutputStream(out).use {
                        it.putNextEntry(ZipEntry("same/2/1/1.png"))
                        it.write(png.toByteArray()); it.closeEntry()
                    }
                }.toByteArray()
            }
            val repo = repository(root)
            val a = repo.import("same", { ByteArrayInputStream(zip(Color.RED)) })
            val b = repo.import("same", { ByteArrayInputStream(zip(Color.BLUE)) })
            val restarted = repository(root)
            val ra = restarted.open(a.id)!!
            val rb = restarted.open(b.id)!!
            fun color(reader: LocalXyzArchive): Int {
                val bytes = requireNotNull(reader.read(2, 1, 1))
                val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                return try { bitmap.getPixel(0, 0) } finally { bitmap.recycle() }
            }
            try {
                assertNotEquals(a.id, b.id)
                assertEquals(Color.RED, color(ra))
                assertEquals(Color.BLUE, color(rb))
                restarted.remove(a.id)
                assertNull(ra.read(2, 1, 1))
                assertEquals(Color.BLUE, color(rb))
                assertEquals(listOf(b), restarted.list())
            } finally { ra.close(); rb.close() }
        } finally { root.deleteRecursively() }
    }
}
