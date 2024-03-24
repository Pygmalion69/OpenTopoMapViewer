package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import org.nitri.opentopo.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem

class ItemizedIconInfoOverlay : ItemizedIconOverlay<OverlayItem?> {
    var infoWindow: WayPointInfoWindow? = null
        private set

    constructor(
        pList: List<OverlayItem?>?,
        pDefaultMarker: Drawable?,
        pOnItemGestureListener: OnItemGestureListener<OverlayItem?>?,
        pContext: Context?
    ) : super(pList, pDefaultMarker, pOnItemGestureListener, pContext)

    constructor(
        pList: List<OverlayItem?>?,
        pOnItemGestureListener: OnItemGestureListener<OverlayItem?>?,
        pContext: Context?
    ) : super(pList, pOnItemGestureListener, pContext)

    constructor(
        pContext: Context?,
        pList: List<OverlayItem?>?,
        pOnItemGestureListener: OnItemGestureListener<OverlayItem?>?
    ) : super(pContext, pList, pOnItemGestureListener)

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        super.draw(canvas, mapView, shadow)
        if (infoWindow != null && infoWindow!!.isOpen) {
            infoWindow!!.draw()
        }
    }

    /**
     * Display a way point info window on the map
     *
     * @param mapView
     * @param item
     */
    fun showWayPointInfo(mapView: MapView?, item: OverlayItem) {
        if (infoWindow != null && infoWindow!!.isOpen) {
            infoWindow!!.close()
        }
        infoWindow = WayPointInfoWindow(
            R.layout.bonuspack_bubble,
            R.id.bubble_title, R.id.bubble_description, R.id.bubble_subdescription, null, mapView
        )
        val windowLocation = item.point as GeoPoint
        infoWindow!!.open(item, windowLocation, 0, 0)
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
        if (infoWindow != null) {
            infoWindow!!.close()
        }
    }
}
