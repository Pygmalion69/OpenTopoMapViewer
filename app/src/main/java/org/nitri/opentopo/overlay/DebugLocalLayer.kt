package org.nitri.opentopo.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import org.nitri.opentopo.R
import org.nitri.opentopo.util.*
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import kotlin.math.*

/** Owns exactly one local-only provider. All attachment and retirement happen on Main. */
internal class LocalMapLayer(
    private val map: MapView,
    private val configuredMaximum: () -> Double,
    private val changed: () -> Unit,
    private val explicitNavigation: () -> Unit,
    private val preferences: LocalMapPreferences = LocalMapPreferences(map.context),
    private val repository: LocalPackageRepository =
        LocalPackageRepository(File(map.context.noBackupFilesDir, "local-xyz"), {})
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var generation = 0
    private var requestedId: String? = null
    private var archive: LocalXyzArchive? = null
    private var overlay: TilesOverlay? = null
    private var provider: MapTileProviderArray? = null
    private var dialog: AlertDialog? = null
    private var controlsJob: Job? = null
    var attribution: String? = null
        private set

    fun refresh() {
        val id = preferences.id.takeIf { preferences.visible }
        if (id == requestedId && overlay != null) {
            alpha()
            applyMaximum()
            return
        }
        retire(restoreMaximum = id == null)
        requestedId = id
        if (id == null) return
        val revision = generation
        job = scope.launch {
            var opened: LocalXyzArchive? = null
            var adopted = false
            try {
                val metadata = repository.lookup(id) ?: error("Package removed")
                // Assign inside IO so cancellation during dispatcher handoff cannot leak a reader.
                withContext(Dispatchers.IO) { opened = repository.open(id) }
                ensureActive()
                if (revision != generation) return@launch
                val local = opened ?: error("Package unavailable")
                val source = XYTileSource(id, local.minZoom, local.maxZoom, 256, ".png", emptyArray())
                val localProvider = MapTileProviderArray(source, null, arrayOf(ArchiveModule(local, source))).apply {
                    setUseDataConnection(false)
                    tileRequestCompleteHandlers.add(map.tileRequestCompleteHandler)
                }
                provider = localProvider
                archive = local
                overlay = object : TilesOverlay(localProvider, map.context) {
                    override fun draw(canvas: Canvas, projection: Projection) {
                        val z = org.osmdroid.util.TileSystem.getInputTileZoomLevel(projection.zoomLevel)
                        if (z in local.zoomLevels) super.draw(canvas, projection)
                    }
                }.apply {
                    loadingBackgroundColor = Color.TRANSPARENT
                    loadingLineColor = Color.TRANSPARENT
                    setOptionsMenuEnabled(false)
                }
                map.overlays.add(0, overlay)
                attribution = metadata.attribution
                adopted = true
                alpha()
                applyMaximum()
                changed()
                if (preferences.navigation == id) {
                    explicitNavigation()
                    map.post {
                        if (revision == generation && archive === local) {
                            navigate(metadata, local)
                            preferences.consumeNavigation()
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LocalMapLayer", "Cannot open local package", e)
                if (revision == generation) {
                    retire()
                    preferences.clear()
                    Toast.makeText(map.context, R.string.local_unavailable, Toast.LENGTH_LONG).show()
                }
            } finally {
                if (!adopted) opened?.close()
            }
        }
    }

    private fun alpha() {
        val a = preferences.opacity / 100f
        overlay?.setColorFilter(ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, a, 0f))))
        map.invalidate()
    }

    private fun applyMaximum() {
        val maximum = maxOf(configuredMaximum(), archive?.maxZoom?.toDouble() ?: 0.0).coerceAtMost(29.0)
        map.maxZoomLevel = maximum
        if (map.zoomLevelDouble > maximum) map.controller.setZoom(maximum)
        map.invalidate()
    }

    private fun navigate(metadata: LocalPackage, local: LocalXyzArchive) {
        map.zoomToBoundingBox(BoundingBox(metadata.north, metadata.east, metadata.south, metadata.west), false, 64)
        val fitted = LocalMapViewport.choose(local.tiles, map.zoomLevelDouble,
            map.mapCenter.latitude, map.mapCenter.longitude)
        map.controller.setZoom(fitted.zoom.toDouble())
        val box = map.boundingBox
        val target = LocalMapViewport.choose(local.tiles, fitted.zoom.toDouble(),
            map.mapCenter.latitude, map.mapCenter.longitude,
            LocalMapBounds(box.lonWest, box.latSouth, box.lonEast, box.latNorth))
        target.fallback?.let { tile ->
            val n = 2.0.pow(tile.z)
            map.controller.setCenter(GeoPoint(
                atan(sinh(PI * (1 - 2 * (tile.y + 0.5) / n))) * 180 / PI,
                (tile.x + 0.5) / n * 360 - 180))
        }
    }

    fun controls() {
        controlsJob?.cancel()
        controlsJob = scope.launch {
            val packages = try { repository.list() } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(map.context, R.string.local_storage_error, Toast.LENGTH_LONG).show()
                return@launch
            }
            val context = map.context
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 8, 24, 8)
            }
            if (packages.isEmpty()) column.addView(TextView(context).apply { setText(R.string.local_empty) })
            val selected = preferences.id
            val visibilitySwitch = androidx.appcompat.widget.SwitchCompat(context).apply {
                setText(R.string.local_visible)
                isChecked = preferences.visible
                setOnCheckedChangeListener { _, checked -> preferences.show(checked); refresh() }
            }
            val choices = RadioGroup(context)
            packages.forEach { p ->
                choices.addView(RadioButton(context).apply {
                    id = android.view.View.generateViewId()
                    text = "${p.displayName}\n${p.id}"
                    isChecked = p.id == selected
                    setOnClickListener { preferences.select(p.id); visibilitySwitch.isChecked = true; refresh() }
                })
            }
            column.addView(choices)
            column.addView(visibilitySwitch)
            val label = TextView(context)
            fun updateLabel() { label.text = context.getString(R.string.local_opacity, preferences.opacity) }
            updateLabel()
            column.addView(label)
            column.addView(SeekBar(context).apply {
                max = 100
                progress = preferences.opacity
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                        if (fromUser) { preferences.opacity(value); updateLabel(); alpha() }
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(bar: SeekBar?) = Unit
                })
            })
            dialog?.dismiss()
            dialog = AlertDialog.Builder(context).setTitle(R.string.local_map)
                .setView(ScrollView(context).apply { addView(column) })
                .setPositiveButton(android.R.string.ok, null).show()
        }
    }

    /** Cancel first; stale IO results are closed by their own finally block. */
    private fun retire(restoreMaximum: Boolean = true) {
        generation++
        job?.cancel()
        job = null
        requestedId = null
        overlay?.let { map.overlays.remove(it) }
        overlay = null
        provider?.tileRequestCompleteHandlers?.clear()
        provider?.detach()
        provider?.clearTileCache()
        provider = null
        archive?.close()
        archive = null
        attribution = null
        if (restoreMaximum) applyMaximum()
        changed()
    }

    fun pause() { controlsJob?.cancel(); dialog?.dismiss(); dialog = null; retire(restoreMaximum = false) }
    fun destroy() { pause(); scope.cancel() }

    private class ArchiveModule(
        private val archive: LocalXyzArchive,
        private val source: ITileSource
    ) : MapTileModuleProviderBase(1, 40) {
        override fun getName() = archive.sourceId
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
        override fun detach() { super.detach(); archive.close() }
    }
}
