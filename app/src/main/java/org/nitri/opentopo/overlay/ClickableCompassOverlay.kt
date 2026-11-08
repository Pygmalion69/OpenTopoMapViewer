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
    mapView: MapView,
    private val onCompassClickListener: OnCompassClickListener
) : CompassOverlay(context, orientationProvider, mapView) {

    private var centerX = 35f
    private var centerY = 35f

    override fun setCompassCenter(x: Float, y: Float) {
        centerX = x
        centerY = y
        super.setCompassCenter(x, y)
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val reuse = Point()
        mapView.projection.rotateAndScalePoint(e.x.toInt(), e.y.toInt(), reuse)
        if (kotlin.math.abs(reuse.x - centerX * mScale) <= mCompassFrameCenterX &&
            kotlin.math.abs(reuse.y - centerY * mScale) <= mCompassFrameCenterY
        ) {
            onCompassClickListener.onCompassClicked()
            return true
        }
        return super.onSingleTapConfirmed(e, mapView)
    }

    interface OnCompassClickListener {
        fun onCompassClicked()
    }
}