package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import org.nitri.opentopo.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem

class ItemizedIconInfoOverlay(
    pList: List<OverlayItem?>?,
    pDefaultMarker: Drawable?,
    pOnItemGestureListener: OnItemGestureListener<OverlayItem?>?,
    pContext: Context?
) : ItemizedIconOverlay<OverlayItem?>(pList, pDefaultMarker, pOnItemGestureListener, pContext) {

    private var infoWindow: WayPointInfoWindow? = null

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        super.draw(canvas, mapView, shadow)
        infoWindow?.takeIf { it.isOpen }?.draw()
    }

    /**
     * Display a way point info window on the map
     *
     * @param mapView
     * @param item
     */
    fun showWayPointInfo(mapView: MapView?, item: OverlayItem) {
        infoWindow?.takeIf { it.isOpen }?.close()

        infoWindow = WayPointInfoWindow(
            R.layout.bonuspack_bubble,
            R.id.bubble_title, R.id.bubble_description, R.id.bubble_subdescription, null, mapView
        ).apply {
            val windowLocation = item.point as GeoPoint
            open(item, windowLocation, 0, 0)
        }
    }

    /**
     * Display a nearby item info window on the map
     *
     * @param mapView
     * @param item
     */
    fun showNearbyItemInfo(mapView: MapView?, item: OverlayItem) {
        showWayPointInfo(mapView, item)
    }

    fun hideWayPointInfo() {
        infoWindow?.takeIf { it.isOpen }?.close()
    }
}
