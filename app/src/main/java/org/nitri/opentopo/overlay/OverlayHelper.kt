package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputLayout
import io.ticofab.androidgpxparser.parser.domain.Gpx
import org.nitri.opentopo.R
import org.nitri.opentopo.SettingsActivity.Companion.PREF_ORS_API_KEY
import org.nitri.opentopo.viewmodel.GpxViewModel
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Marker.OnMarkerDragListener
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace
import org.osmdroid.views.overlay.TilesOverlay

class OverlayHelper(private val mContext: Context, private val mMapView: MapView?) {
    private var wayPointOverlay: ItemizedIconInfoOverlay? = null
    private var nearbyItemOverlay: ItemizedIconInfoOverlay? = null
    private var overlayType = OVERLAY_NONE
    private var overlayTileProvider: MapTileProviderBasic? = null
    private var tilesOverlay: TilesOverlay? = null
    private var gpxViewModel: GpxViewModel? = null
    private val tileOverlayAlphaMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 0.8f, 0f
        )
    )
    private val tileOverlayAlphaFilter = ColorMatrixColorFilter(tileOverlayAlphaMatrix)

    private val wayPointItemGestureListener: OnItemGestureListener<OverlayItem?> =
        object : OnItemGestureListener<OverlayItem?> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                wayPointOverlay?.let { overlay ->
                    mMapView?.let { mapView ->
                        item?.let { currentItem ->
                            overlay.showWayPointInfo(mapView, currentItem)
                        }
                    }
                }
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                return false
            }
        }

    private lateinit var markerInteractionListener: MarkerInteractionListener
    private val onMarkerDragListener : OnMarkerDragListener = object : OnMarkerDragListener {
        override fun onMarkerDrag(marker: Marker?) {
            //NOP
        }

        override fun onMarkerDragEnd(marker: Marker?) {
            marker?.let {
                val markerModel = it.relatedObject as MarkerModel
                markerModel.latitude = it.position.latitude
                markerModel.longitude = it.position.longitude
            }
        }

        override fun onMarkerDragStart(marker: Marker?) {
            //NOP
        }

    }

    private val onMarkerClickListener : CustomMarker.OnCustomMarkerClickListener =
        object : CustomMarker.OnCustomMarkerClickListener {
            override fun onMarkerClick(marker: CustomMarker?): Boolean {
                markerInfoWindow?.takeIf { it.isOpen }?.close()

                val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
                val orsEnabled = !prefs.getString(PREF_ORS_API_KEY, null).isNullOrEmpty()

                markerInfoWindow = MarkerInfoWindow(
                    R.layout.custom_bubble,
                    R.id.bubble_title, R.id.bubble_description, R.id.bubble_subdescription, null,
                    mMapView, orsEnabled
                ).apply {
                    onMarkerInfoEditClickListener = marker?.onMarkerInfoEditClickListener
                    onMarkerWaypointClickListener = marker?.onMarkerWaypointClickListener
                    marker?.position?.let {
                        val windowLocation =
                            GeoPoint(it.latitude, it.longitude)
                        open(marker.relatedObject, windowLocation, 0, 0)
                    }
                }
                return true
            }

        }

    private val onMarkerInfoEditClickListener : MarkerInfoWindow.OnMarkerInfoEditClickListener =
        object : MarkerInfoWindow.OnMarkerInfoEditClickListener {
            override fun onMarkerInfoEditClick(markerModel: MarkerModel) {
                val dialogView =
                    LayoutInflater.from(mContext).inflate(R.layout.dialog_edit_marker, null)
                val nameInput = dialogView.findViewById<TextInputLayout>(R.id.nameInput)
                val descriptionInput =
                    dialogView.findViewById<TextInputLayout>(R.id.descriptionInput)

                nameInput.editText?.setText(markerModel.name)
                descriptionInput.editText?.setText(markerModel.description)

                val alertDialog = AlertDialog.Builder(mContext)
                    .setTitle(mContext.getString(R.string.edit_marker))
                    .setView(dialogView)
                    .setPositiveButton(mContext.getString(R.string.ok)) { _, _ ->
                        markerModel.name = nameInput.editText?.text.toString()
                        markerModel.description = descriptionInput.editText?.text.toString()
                        markerInteractionListener.onMarkerUpdate(markerModel)
                    }
                    .setNegativeButton(mContext.getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNeutralButton(mContext.getString(R.string.delete)) { _, _ ->
                        showDeleteConfirmationDialog(mContext) {
                            if (markerModel.routeWaypoint) {
                                removeWaypoint(markerModel)
                            }
                            markerInteractionListener.onMarkerDelete(markerModel)
                        }
                    }
                    .create()
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                alertDialog.show()

            }
        }

    private val onMarkerWaypointClickListener : MarkerInfoWindow.OnMarkerWaypointClickListener =
        object : MarkerInfoWindow.OnMarkerWaypointClickListener {
            override fun onMarkerWaypointAddClick(markerModel: MarkerModel) {
                addWaypoint(markerModel)
            }

            override fun onMarkerWaypointRemoveClick(markerModel: MarkerModel) {
                removeWaypoint(markerModel)
            }
        }

    private fun showDeleteConfirmationDialog(context: Context, onDeleteConfirmed: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(mContext.getString(R.string.confirm_delete))
            .setMessage(mContext.getString(R.string.prompt_confirm_delete))
            .setPositiveButton(mContext.getString(R.string.delete)) { _, _ ->
                onDeleteConfirmed()
            }
            .setNegativeButton(mContext.getString(R.string.cancel), null)
            .create()
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        alertDialog.show()
    }

    private val nearbyItemGestureListener: OnItemGestureListener<OverlayItem?> =
        object : OnItemGestureListener<OverlayItem?> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                nearbyItemOverlay?.let { overlay ->
                    mMapView?.let { mapView ->
                        item?.let { currentItem ->
                            overlay.showNearbyItemInfo(mapView, currentItem)
                        }
                    }
                }
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                clearNearby()
                mMapView?.invalidate()
                return true
            }
        }

    private var markerInfoWindow: MarkerInfoWindow? = null

    private var trackOverlay: TrackOverlay? = null
    private val mapMarkers = ArrayList<Marker>()

    /**
     * Add GPX as an overlay
     *
     * @param gpx
     * @see Gpx
     */
    fun setGpx(gpx: Gpx) {
        clearGpx()

        val tracks = gpx.tracks
        val routes = gpx.routes

        if (!tracks.isNullOrEmpty()) {
            tracks.forEach { track ->
                trackOverlay = TrackOverlay(mContext, track)
                mMapView?.overlays?.add(0, trackOverlay)
            }
        }

        val wayPointItems = mutableListOf<OverlayItem>()

        gpx.wayPoints?.forEach { wayPoint ->
            val gp = GeoPoint(wayPoint.latitude, wayPoint.longitude)
            val item = OverlayItem(wayPoint.name, wayPoint.desc, gp).apply {
                setMarker(ContextCompat.getDrawable(mContext, R.drawable.map_marker))
                markerHotspot = HotspotPlace.BOTTOM_CENTER
            }
            wayPointItems.add(item)
        }

        if (!routes.isNullOrEmpty()) {
            routes.forEach { route ->
                route.routePoints?.forEach { rtePt ->
                    val gp = GeoPoint(rtePt.latitude, rtePt.longitude)
                    val item = OverlayItem(rtePt.name, rtePt.desc, gp).apply {
                        setMarker(ContextCompat.getDrawable(mContext, R.drawable.route_point_marker))
                        markerHotspot = HotspotPlace.CENTER
                    }
                    wayPointItems.add(item)
                }
            }
        }

        if (wayPointItems.isNotEmpty()) {
            wayPointOverlay = ItemizedIconInfoOverlay(
                wayPointItems,
                ContextCompat.getDrawable(mContext, R.drawable.map_marker),
                wayPointItemGestureListener,
                mContext
            )
            mMapView?.overlays?.add(wayPointOverlay)
        }
        mMapView?.invalidate()
    }

    /**
     * Remove GPX layer
     */
    fun clearGpx() {
        mMapView?.let { mapView ->
            trackOverlay?.also {
                mapView.overlays.remove(it)
                trackOverlay = null
            }
            wayPointOverlay?.also {
                mapView.overlays.remove(it)
                wayPointOverlay = null
            }
        }
    }

    /**
     * Set the collection of user markers
     */
    fun setMarkers(markers: List<MarkerModel>, listener: MarkerInteractionListener) {
        clearMarkers()
        mMapView?.let { mapView ->
            markerInteractionListener = listener
            markers.forEach {
                val mapMarker = CustomMarker(mapView)
                mapMarker.position = GeoPoint(it.latitude, it.longitude)
                mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapMarker.title = it.name
                mapMarker.id = it.toString()
                mapMarker.relatedObject = it
                mapMarker.isDraggable = true
                mapMarker.icon = ContextCompat.getDrawable(mContext, R.drawable.map_marker)
                mapMarker.setOnMarkerDragListener(onMarkerDragListener)
                mapMarker.onCustomMarkerClickListener = onMarkerClickListener
                mapMarker.onMarkerInfoEditClickListener = onMarkerInfoEditClickListener
                mapMarker.onMarkerWaypointClickListener = onMarkerWaypointClickListener
                mapMarkers.add(mapMarker)
                mMapView.overlays?.add(mapMarker)
            }
        }
    }

    /**
     * Remove user markers
     */
    private fun clearMarkers() {
        mMapView?.let { mapView ->
            mapMarkers.forEach {
                mapView.overlays.remove(it)
            }
        }
    }

    @Deprecated(message ="Use MarkerViewModel instead", level = DeprecationLevel.WARNING)
    fun setNearby(item: NearbyItem) {
        clearNearby()
        val geoPoint = GeoPoint(item.lat, item.lon)
        val mapItem = OverlayItem(item.title, item.description, geoPoint)
        nearbyItemOverlay = ItemizedIconInfoOverlay(
            ArrayList(listOf(mapItem)),
            ContextCompat.getDrawable(mContext, R.drawable.ic_default_marker),
            nearbyItemGestureListener,
            mContext
        )
        mMapView?.overlays?.add(nearbyItemOverlay)
        mMapView?.invalidate()
    }

    /**
     * Remove nearby item layer
     */
    fun clearNearby() {
        if (mMapView != null && nearbyItemOverlay != null) {
            mMapView.overlays.remove(nearbyItemOverlay)
            nearbyItemOverlay = null
        }
    }

    /**
     * Returns whether a GPX has been added
     *
     * @return GPX layer present
     */
    fun hasGpx(): Boolean {
        return trackOverlay != null || wayPointOverlay != null
    }

    fun setTilesOverlay(overlay: Int) {
        overlayType = overlay
        var overlayTiles: ITileSource? = null
        tilesOverlay?.let {
            mMapView?.overlays?.remove(it)
        }
        when (overlayType) {
            OVERLAY_NONE -> {}
            OVERLAY_HIKING -> overlayTiles = XYTileSource(
                "hiking", 1, 17, 256, ".png", arrayOf(
                    "https://tile.waymarkedtrails.org/hiking/"
                ), mContext.getString(R.string.lonvia_copy)
            )

            OVERLAY_CYCLING -> overlayTiles = XYTileSource(
                "cycling", 1, 17, 256, ".png", arrayOf(
                    "https://tile.waymarkedtrails.org/cycling/"
                ), mContext.getString(R.string.lonvia_copy)
            )
        }
        overlayTiles?.let { tiles ->
            val tileProvider = MapTileProviderBasic(mContext).apply {
                setTileSource(tiles)
                tileRequestCompleteHandlers.clear()
                tileRequestCompleteHandlers.add(mMapView?.tileRequestCompleteHandler)
            }

            tilesOverlay = TilesOverlay(tileProvider, mContext).apply {
                loadingBackgroundColor = Color.TRANSPARENT
                setColorFilter(tileOverlayAlphaFilter)
            }
            mMapView?.overlays?.add(0, tilesOverlay)
        }
        mMapView?.invalidate()
    }

    val copyrightNotice: String?
        /**
         * Copyright notice for the tile overlay
         *
         * @return copyright notice or null
         */
        get() = if (overlayTileProvider != null && overlayType != OVERLAY_NONE) {
            overlayTileProvider?.tileSource?.copyrightNotice
        } else null

    fun destroy() {
        tilesOverlay = null
        overlayTileProvider = null
        wayPointOverlay = null
    }

    private fun addWaypoint(markerModel: MarkerModel) {
        markerModel.routeWaypoint = true
        markerInteractionListener.onMarkerUpdate(markerModel)
        markerInteractionListener.onMarkerWaypointsChanged()
    }

    private fun removeWaypoint(markerModel: MarkerModel) {
       markerModel.routeWaypoint = false
        markerInteractionListener.onMarkerUpdate(markerModel)
        markerInteractionListener.onMarkerWaypointsChanged()
    }

    interface MarkerInteractionListener {
        fun onMarkerMoved(markerModel: MarkerModel)
        fun onMarkerClicked(markerModel: MarkerModel)
        fun onMarkerDelete(markerModel: MarkerModel)
        fun onMarkerUpdate(markerModel: MarkerModel)
        fun onMarkerWaypointsChanged()
    }
    companion object {

        val TAG : String = OverlayHelper::class.java.simpleName

        /**
         * Tiles Overlays
         */
        const val OVERLAY_NONE = 1
        const val OVERLAY_HIKING = 2
        const val OVERLAY_CYCLING = 3
    }
}
