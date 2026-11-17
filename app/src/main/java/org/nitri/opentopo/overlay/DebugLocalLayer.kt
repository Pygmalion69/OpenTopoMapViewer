package org.nitri.opentopo.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import kotlinx.coroutines.*
import org.nitri.opentopo.BuildConfig
import org.nitri.opentopo.util.LocalXyzArchive
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

/** Debug staging hook only: no importer, shared cache, archive discovery or network fallback. */
internal class DebugLocalLayer(private val map: MapView) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: TilesOverlay? = null
    var maxZoom: Int? = null
        private set

    fun start() {
        if (!BuildConfig.DEBUG) return
        scope.launch {
            var archive: LocalXyzArchive? = null
            var adopted = false
            try {
                withContext(Dispatchers.IO) {
                    val directory = File(map.context.filesDir, "local-layers/caperrino-proof")
                    if (File(directory, "enabled").isFile) {
                        archive = LocalXyzArchive(File(directory, "tiles.zip"))
                    }
                }
                val local = archive ?: return@launch
                val source = XYTileSource("local-caperrino-proof", local.minZoom,
                    local.maxZoom, 256, ".png", emptyArray())
                val module = ArchiveModule(local, source)
                val provider = MapTileProviderArray(source, null, arrayOf(module)).apply {
                    setUseDataConnection(false)
                    tileRequestCompleteHandlers.add(map.tileRequestCompleteHandler)
                }
                overlay = object : TilesOverlay(provider, map.context) {
                    override fun draw(canvas: Canvas, projection: Projection) {
                        // Do not stretch cached tiles into zooms absent from the package.
                        val z = org.osmdroid.util.TileSystem.getInputTileZoomLevel(projection.zoomLevel)
                        if (z in local.minZoom..local.maxZoom) super.draw(canvas, projection)
                    }
                }.apply {
                    loadingBackgroundColor = Color.TRANSPARENT
                    loadingLineColor = Color.TRANSPARENT
                    setOptionsMenuEnabled(false)
                    setColorFilter(ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0.65f, 0f))))
                }
                map.overlays.add(0, overlay)
                maxZoom = local.maxZoom
                map.maxZoomLevel = maxOf(map.maxZoomLevel, local.maxZoom.toDouble())
                adopted = true
                Log.i("DebugLocalLayer", "local-caperrino-proof: ${local.tileCount} tiles, zoom ${local.minZoom}–${local.maxZoom}")
                map.invalidate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DebugLocalLayer", "Cannot open staged local layer", e)
            } finally {
                if (!adopted) archive?.close()
            }
        }
    }

    fun destroy() {
        scope.cancel()
        overlay?.let {
            map.overlays.remove(it)
            it.onDetach(map)
        }
        overlay = null
        maxZoom = null
    }

    private class ArchiveModule(
        private val archive: LocalXyzArchive,
        private val source: ITileSource
    ) : MapTileModuleProviderBase(1, 40) {
        override fun getName() = "local-caperrino-proof"
        override fun getThreadGroupName() = "local-xyz"
        override fun getUsesDataConnection() = false
        override fun getMinimumZoomLevel() = archive.minZoom
        override fun getMaximumZoomLevel() = archive.maxZoom
        override fun setTileSource(tileSource: ITileSource?) = Unit
        override fun getTileLoader(): TileLoader = object : TileLoader() {
            override fun loadTile(index: Long) = archive.read(
                MapTileIndex.getZoom(index), MapTileIndex.getX(index), MapTileIndex.getY(index)
            )?.inputStream()?.use { source.getDrawable(it) }
        }
        override fun detach() {
            super.detach()
            archive.close()
        }
    }
}
