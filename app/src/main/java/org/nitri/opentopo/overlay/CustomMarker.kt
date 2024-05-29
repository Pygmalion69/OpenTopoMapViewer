package org.nitri.opentopo.overlay

import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class CustomMarker(mapView: MapView) : Marker(mapView) {

    var onMarkerLongPressListener: OnMarkerLongPressListener? = null

    override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
        onMarkerLongPressListener?.let { listener ->
            event?.let {
                if (hitTest(it, mapView)) {
                    listener.onMarkerLongPress(this)
                    return true
                }
            }
        }
        return super.onLongPress(event, mapView)
    }

    interface OnMarkerLongPressListener {
        fun onMarkerLongPress(marker: Marker)
    }

}