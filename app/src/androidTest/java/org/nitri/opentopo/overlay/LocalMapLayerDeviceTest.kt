package org.nitri.opentopo.overlay

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.nitri.opentopo.util.*
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Run on a disposable emulator. Fixtures use an isolated store and preferences. */
class LocalMapLayerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun reader(layer: LocalMapLayer): LocalXyzArchive? =
        LocalMapLayer::class.java.getDeclaredField("archive").apply { isAccessible = true }.get(layer) as LocalXyzArchive?

    private fun zip(z: Int, color: Int): ByteArray {
        val png = ByteArrayOutputStream()
        Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also {
            try { it.eraseColor(color); it.compress(Bitmap.CompressFormat.PNG, 100, png) }
            finally { it.recycle() }
        }
        return ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use {
                it.putNextEntry(ZipEntry("$z/0/0.png")); it.write(png.toByteArray()); it.closeEntry()
                it.putNextEntry(ZipEntry("attribution.txt")); it.write("Fixture".toByteArray()); it.closeEntry()
            }
        }.toByteArray()
    }

    /** Optional picker-imported fixture; the ZIP itself is never packaged in the APK. */
    @Test fun suppliedPackageRendersAt18AndSurvivesRecreation() = runBlocking {
        val id = InstrumentationRegistry.getArguments().getString("localPackageId")
        org.junit.Assume.assumeNotNull(id)
        if (InstrumentationRegistry.getArguments().getString("clearMapCache") == "true") {
            // This opt-in branch is destructive to the background cache: emulator only.
            check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
            val legacyFile = File(context.cacheDir, "step3-legacy.zip")
            val legacyResult = try {
                legacyFile.writeBytes(zip(2, Color.RED))
                importOpenTopoMapZipToSqliteCache(context, android.net.Uri.fromFile(legacyFile))
            } finally { legacyFile.delete() }
            assertNull(legacyResult.error)
            assertEquals(1, legacyResult.imported)
            assertEquals(0, legacyResult.skipped)
            val legacySource = org.osmdroid.tileprovider.tilesource.XYTileSource(
                "OpenTopoMap", 0, 17, 256, ".png", emptyArray())
            val legacyIndex = org.osmdroid.util.MapTileIndex.getTileIndex(2, 0, 0)
            assertTrue(org.osmdroid.tileprovider.modules.SqlTileWriter().exists(legacySource, legacyIndex))
            val before = MapCache.revision.value
            androidx.test.core.app.ActivityScenario.launch(org.nitri.opentopo.SettingsActivity::class.java).use { settings ->
                settings.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.fragments
                        .filterIsInstance<org.nitri.opentopo.SettingsActivity.SettingsFragment>().single()
                    fragment.findPreference<androidx.preference.Preference>("clear_map_cache")!!.performClick()
                }
                androidx.test.espresso.Espresso.onView(
                    androidx.test.espresso.matcher.ViewMatchers.withText(org.nitri.opentopo.R.string.clear_map_cache_confirm))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click())
                withTimeout(10000) {
                    while (MapCache.revision.value == before || MapCache.clearing.value) delay(20)
                }
            }
            assertFalse(org.osmdroid.tileprovider.modules.SqlTileWriter().exists(legacySource, legacyIndex))
        }
        val preferences = LocalMapPreferences(context)
        preferences.select(requireNotNull(id))
        preferences.opacity(65)
        androidx.test.core.app.ActivityScenario.launch(org.nitri.opentopo.MainActivity::class.java).use { scenario ->
            lateinit var layer: LocalMapLayer
            lateinit var map: MapView
            fun acquire() {
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager
                        .findFragmentById(org.nitri.opentopo.R.id.map_container)!!
                    layer = fragment.javaClass.getDeclaredField("localMapLayer")
                        .apply { isAccessible = true }.get(fragment) as LocalMapLayer
                    map = activity.findViewById(org.nitri.opentopo.R.id.mapview)
                }
            }
            suspend fun awaitReader() = withTimeout(10000) {
                while (true) {
                    var ready = false
                    main { ready = reader(layer)?.sourceId == id }
                    if (ready) break
                    delay(20)
                }
            }
            acquire()
            awaitReader()
            lateinit var original: LocalXyzArchive
            main {
                original = reader(layer)!!
                assertEquals(1692, original.tileCount)
                assertEquals(18.0, map.maxZoomLevel, 0.0)
                val tile = original.tiles.first { it.z == 18 }
                val n = Math.pow(2.0, tile.z.toDouble())
                map.controller.setZoom(18.0)
                map.controller.setCenter(org.osmdroid.util.GeoPoint(
                    Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(Math.PI * (1 - 2 * (tile.y + 0.5) / n)))),
                    (tile.x + 0.5) / n * 360 - 180))
                assertEquals(18.0, map.zoomLevelDouble, 0.0)
            }
            // Exercise real tile requests before taking pixel evidence.
            withTimeout(10000) {
                while (true) {
                    var loaded = false
                    main {
                        val tile = original.tiles.first { it.z == 18 }
                        val provider = layer.javaClass.getDeclaredField("provider")
                            .apply { isAccessible = true }.get(layer) as org.osmdroid.tileprovider.MapTileProviderArray
                        loaded = provider.getMapTile(org.osmdroid.util.MapTileIndex.getTileIndex(tile.z, tile.x, tile.y)) != null
                    }
                    if (loaded) break
                    delay(20)
                }
            }
            val pixels = mutableListOf<IntArray>()
            for (opacity in listOf(0, 65, 100)) {
                main {
                    preferences.opacity(opacity)
                    layer.refresh()
                    assertSame(original, reader(layer))
                    val bitmap = Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888)
                    try {
                        map.draw(android.graphics.Canvas(bitmap))
                        File(context.getExternalFilesDir(null), "step3-opacity-$opacity.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        val sample = IntArray(100 * 100)
                        bitmap.getPixels(sample, 0, 100, map.width / 2 - 50, map.height / 2 - 50, 100, 100)
                        pixels.add(sample)
                    } finally { bitmap.recycle() }
                }
            }
            assertFalse(pixels[0].contentEquals(pixels[1]))
            assertFalse(pixels[1].contentEquals(pixels[2]))
            scenario.recreate()
            acquire()
            awaitReader()
            main {
                assertEquals(id, preferences.id)
                assertTrue(preferences.visible)
                assertEquals(100, preferences.opacity)
                assertEquals(18.0, map.maxZoomLevel, 0.0)
                preferences.show(false)
                layer.refresh()
                assertEquals(17.0, map.maxZoomLevel, 0.0)
                assertTrue(map.zoomLevelDouble <= 17.0)
                preferences.show(true)
                layer.refresh()
            }
            awaitReader()
        }
    }

    @Test fun rapidSwitchOpacityHideRemovalAndTeardown() = runBlocking {
        val key = "local-layer-test-" + UUID.randomUUID()
        val root = File(context.noBackupFilesDir, key)
        val rawPrefs = context.getSharedPreferences(key, 0)
        val preferences = LocalMapPreferences(rawPrefs)
        val repository = LocalPackageRepository(root, {})
        val a = repository.import("same", { ByteArrayInputStream(zip(18, Color.RED)) })
        val b = repository.import("same", { ByteArrayInputStream(zip(16, Color.BLUE)) })
        lateinit var map: MapView
        lateinit var layer: LocalMapLayer
        main {
            map = MapView(context)
            map.layout(0, 0, 1000, 1000)
            layer = LocalMapLayer(map, { 17.0 }, {}, {}, preferences, repository)
        }
        suspend fun awaitId(id: String?) = withTimeout(5000) {
            while (true) {
                var actual: String? = null
                main { actual = reader(layer)?.sourceId }
                if (actual == id) break
                delay(20)
            }
        }
        try {
            assertEquals(65, preferences.opacity)
            main {
                repeat(10) { preferences.select(a.id); layer.refresh(); preferences.select(b.id); layer.refresh() }
                preferences.select(a.id); layer.refresh()
            }
            awaitId(a.id)
            lateinit var first: LocalXyzArchive
            main {
                first = reader(layer)!!
                assertEquals(18.0, map.maxZoomLevel, 0.0)
                assertEquals("Fixture", layer.attribution)
                val raster = map.overlays[0]
                MapCache.resetProvider(map)
                assertSame(raster, map.overlays[0])
                assertSame(first, reader(layer))
                map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                assertSame(raster, map.overlays[0])
                assertSame(first, reader(layer))
                for (opacity in listOf(0, 40, 100)) {
                    preferences.opacity(opacity); layer.refresh()
                    assertSame(first, reader(layer))
                }
                val hiking = OverlayHelper(context, map)
                hiking.setTilesOverlay(OverlayHelper.OVERLAY_HIKING)
                assertTrue(map.overlays[0] is TilesOverlay)
                assertEquals(2, map.overlays.count { it is TilesOverlay })
                hiking.setTilesOverlay(OverlayHelper.OVERLAY_NONE)
                map.controller.setZoom(18.0)
                preferences.show(false); layer.refresh()
                assertEquals(a.id, preferences.id)
                assertNull(reader(layer))
                assertNull(first.read(18, 0, 0))
                assertEquals(17.0, map.maxZoomLevel, 0.0)
                assertTrue(map.zoomLevelDouble <= 17.0)
                assertNull(layer.attribution)
                preferences.show(true); layer.refresh()
            }
            awaitId(a.id)
            main { layer.pause() }
            repository.remove(a.id)
            main { layer.refresh() }
            withTimeout(5000) { while (preferences.id != null) delay(20) }
            main {
                assertNull(reader(layer))
                assertEquals(17.0, map.maxZoomLevel, 0.0)
                preferences.select(b.id); layer.refresh()
            }
            awaitId(b.id)
            assertEquals(listOf(b.id), repository.list().map { it.id })
            main {
                val second = reader(layer)!!
                layer.destroy()
                assertNull(second.read(16, 0, 0))
                assertTrue(map.overlays.none { it is TilesOverlay })
            }
            // A new preferences wrapper preserves all three fields, with no automatic selection.
            assertEquals(b.id, LocalMapPreferences(rawPrefs).id)
            assertEquals(100, LocalMapPreferences(rawPrefs).opacity)
            assertTrue(LocalMapPreferences(rawPrefs).visible)
        } finally {
            main { layer.destroy(); map.onDetach() }
            rawPrefs.edit().clear().commit()
            root.deleteRecursively()
        }
    }
}
