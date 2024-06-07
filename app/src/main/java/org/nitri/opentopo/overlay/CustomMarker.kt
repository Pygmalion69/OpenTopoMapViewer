package org.nitri.opentopo.overlay

import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class CustomMarker(mapView: MapView) : Marker(mapView) {

    var onMarkerInfoEditClickListener: MarkerInfoWindow.OnMarkerInfoEditClickListener? = null
    var onCustomMarkerClickListener: OnCustomMarkerClickListener? = null

    override fun onSingleTapConfirmed(event: MotionEvent?, mapView: MapView?): Boolean {
        val touched = hitTest(event, mapView)
        return if (touched) {
            onCustomMarkerClickListener?.onMarkerClick(this) ?: onMarkerClickDefault(this, mapView)
        } else {
            false
        }
    }

    interface OnMarkerLongPressListener {
        fun onMarkerLongPress(marker: Marker)
    }
    interface OnCustomMarkerClickListener{
        fun onMarkerClick(marker: CustomMarker?): Boolean
    }

}