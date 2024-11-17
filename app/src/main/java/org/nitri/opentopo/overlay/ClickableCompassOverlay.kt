package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Point
import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

class ClickableCompassOverlay(
    context: Context,
    orientationProvider: InternalCompassOrientationProvider,
    mapView: MapView
) : CompassOverlay(context, orientationProvider, mapView) {


    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val reuse = Point()
        mapView.projection.rotateAndScalePoint(e.x.toInt(), e.y.toInt(), reuse)
        if (reuse.x < mCompassFrameCenterX * 2 && reuse.y < mCompassFrameCenterY * 2) {
            mapView.controller?.animateTo(null, null, 300, 0f)
            return true
        }

        return super.onSingleTapConfirmed(e, mapView)
    }

}