package org.nitri.opentopo.util

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.preference.Preference
import org.nitri.opentopo.SettingsActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class MapCacheTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var originalCache: File
    private lateinit var testRoot: File
    private val tile = MapTileIndex.getTileIndex(1, 0, 0)
    private val downloaded = XYTileSource("downloaded", 0, 17, 256, ".png", emptyArray())
    private val imported = XYTileSource("OpenTopoMap", 0, 17, 256, ".png", emptyArray())

    @Before fun setup() {
        originalCache = Configuration.getInstance().osmdroidTileCache
        testRoot = File(context.cacheDir, "cache-clear-test-${System.nanoTime()}").apply { mkdirs() }
        SqlTileWriter.setCleanupOnStart(false)
        SqlTileWriter().refreshDb()
        Configuration.getInstance().osmdroidTileCache = File(testRoot, "active")
    }

    @After fun cleanup() {
        SqlTileWriter().refreshDb()
        Configuration.getInstance().osmdroidTileCache = originalCache
        SqlTileWriter.setCleanupOnStart(true)
        testRoot.deleteRecursively()
    }

    @Test fun removesAllSourcesAndPreservesOtherFilesAndRejectsOldDownloads() = runBlocking {
        val writer = MapCache.GuardedWriter()
        // osmdroid 6.1.20 saveFile always returns false, even after a successful insert.
        writer.saveFile(downloaded, tile, byteArrayOf(1, 2).inputStream(), Long.MAX_VALUE)
        val zip = File(testRoot, "original.zip")
        ZipOutputStream(zip.outputStream()).use {
            it.putNextEntry(ZipEntry("OpenTopoMap/1/0/0.png"))
            it.write(byteArrayOf(3, 4))
            it.closeEntry()
        }
        assertEquals(1, importOpenTopoMapZipToSqliteCache(context, Uri.fromFile(zip)).imported)
        assertTrue(writer.exists(downloaded, tile))
        assertTrue(writer.exists(imported, tile))
        val originals = listOf(zip, File(testRoot, "original.gpx").apply { writeText("gpx") },
            File(testRoot, "inactive/cache.db").apply { parentFile!!.mkdirs(); writeText("inactive") },
            File(Configuration.getInstance().osmdroidTileCache, "unrelated.txt").apply { writeText("keep") })
        val before = originals.map { it.readBytes().toList() }

        assertTrue(synchronized(MapCache.lock) { MapCache.purgeLocked() })
        assertFalse(writer.exists(downloaded, tile))
        assertFalse(writer.exists(imported, tile))
        writer.saveFile(downloaded, tile, byteArrayOf(5).inputStream(), Long.MAX_VALUE)
        assertFalse(writer.exists(downloaded, tile))
        val fresh = MapCache.GuardedWriter()
        fresh.saveFile(downloaded, tile, byteArrayOf(6).inputStream(), Long.MAX_VALUE)
        assertTrue(fresh.exists(downloaded, tile))
        assertEquals(before, originals.map { it.readBytes().toList() })
    }

    @Test fun cancellingConfirmationLeavesTilesUntouched() {
        val writer = MapCache.GuardedWriter()
        writer.saveFile(downloaded, tile, byteArrayOf(1).inputStream(), Long.MAX_VALUE)
        val generation = MapCache.generation
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            Espresso.onIdle()
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.fragments
                    .filterIsInstance<SettingsActivity.SettingsFragment>().single()
                fragment.findPreference<Preference>("clear_map_cache")!!.performClick()
            }
            Espresso.onView(withText(android.R.string.cancel))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                .perform(click())
            assertTrue(writer.exists(downloaded, tile))
            assertEquals(generation, MapCache.generation)
        }
    }

    @Test fun providerResetDropsMemoryAndPreservesMapState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val map = org.osmdroid.views.MapView(context)
            try {
                map.layout(0, 0, 400, 400)
                map.setTileProvider(MapCache.provider(context, downloaded))
                map.setUseDataConnection(false)
                map.controller.setZoom(8.0)
                map.controller.setCenter(org.osmdroid.util.GeoPoint(48.0, 11.0))
                val overlay = object : org.osmdroid.views.overlay.Overlay() {}
                map.overlays.add(overlay)
                val center = map.mapCenter
                val zoom = map.zoomLevelDouble
                val oldProvider = map.tileProvider
                oldProvider.tileCache.putTile(tile, android.graphics.drawable.BitmapDrawable(
                    context.resources, android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                ))
                assertTrue(oldProvider.tileCache.containsTile(tile))
                MapCache.resetProvider(map)
                assertNotSame(oldProvider, map.tileProvider)
                assertFalse(map.tileProvider.tileCache.containsTile(tile))
                assertNull(map.tileProvider.getMapTile(tile))
                assertFalse(map.useDataConnection())
                assertSame(downloaded, map.tileProvider.tileSource)
                assertTrue(map.overlays.contains(overlay))
                assertEquals(zoom, map.zoomLevelDouble, 0.0)
                assertEquals(center.latitude, map.mapCenter.latitude, 0.00001)
                assertEquals(center.longitude, map.mapCenter.longitude, 0.00001)
            } finally {
                map.onDetach()
            }
        }
    }

    @Test fun purgeWaitsForAnInflightDownloadWrite() {
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val purging = java.util.concurrent.CountDownLatch(1)
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        val writer = MapCache.GuardedWriter()
        try {
            val input = object : java.io.ByteArrayInputStream(byteArrayOf(1, 2)) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    entered.countDown()
                    check(release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                    return super.read(buffer, offset, length)
                }
            }
            val write = pool.submit { writer.saveFile(downloaded, tile, input, Long.MAX_VALUE) }
            assertTrue(entered.await(10, java.util.concurrent.TimeUnit.SECONDS))
            val purge = pool.submit<Boolean> {
                purging.countDown()
                synchronized(MapCache.lock) { MapCache.purgeLocked() }
            }
            assertTrue(purging.await(10, java.util.concurrent.TimeUnit.SECONDS))
            assertFalse(purge.isDone)
            release.countDown()
            write.get(10, java.util.concurrent.TimeUnit.SECONDS)
            assertTrue(purge.get(10, java.util.concurrent.TimeUnit.SECONDS))
            assertFalse(writer.exists(downloaded, tile))
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test fun mismatchedActiveDatabaseFailsWithoutDeletingTiles() {
        val writer = MapCache.GuardedWriter()
        writer.saveFile(downloaded, tile, byteArrayOf(1).inputStream(), Long.MAX_VALUE)
        Configuration.getInstance().osmdroidTileCache = File(testRoot, "different")
        assertFalse(synchronized(MapCache.lock) { MapCache.purgeLocked() })
        assertTrue(writer.exists(downloaded, tile))
        assertFalse(File(testRoot, "different/cache.db").exists())
    }

    @Test fun unavailableStorageDoesNotReportSuccess() {
        SqlTileWriter().refreshDb()
        val notADirectory = File(testRoot, "file").apply { writeText("preserve") }
        Configuration.getInstance().osmdroidTileCache = notADirectory
        assertFalse(synchronized(MapCache.lock) { MapCache.purgeLocked() })
        assertEquals("preserve", notADirectory.readText())
    }
}
