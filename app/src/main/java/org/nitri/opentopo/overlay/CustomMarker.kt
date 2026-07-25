package org.nitri.opentopo.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker

class CustomMarker(private val mapView: MapView) : Marker(mapView) {

    var labelText: String = ""
    var labelVisible: Boolean = false
    var minimumLabelZoom: Double = 14.0

    private val annotationRenderer = MarkerAnnotationRenderer(mapView.context)

    var onMarkerInfoEditClickListener: MarkerInfoWindow.OnMarkerInfoEditClickListener? = null
    var onMarkerWaypointClickListener: MarkerInfoWindow.OnMarkerWaypointClickListener? = null
    var onCustomMarkerClickListener: OnCustomMarkerClickListener? = null

    override fun draw(canvas: Canvas, pj: Projection) {
        super.draw(canvas, pj)

        if (!labelVisible || labelText.isBlank() || (pj.zoomLevel < minimumLabelZoom) || !isDisplayed) {
            annotationRenderer.clear()
            return
        }

        val x = mPositionPixels.x.toFloat()
        val y = mPositionPixels.y.toFloat()

        annotationRenderer.draw(canvas, pj, x, y, labelText, mIcon.intrinsicHeight, mAnchorV, mapView.width, mapView.height)
    }

    override fun hitTest(event: MotionEvent, mapView: MapView): Boolean {
        if (super.hitTest(event, mapView)) {
            return true
        }

        if (!labelVisible || labelText.isBlank() || (mapView.zoomLevelDouble < minimumLabelZoom)) {
            return false
        }

        return annotationRenderer.hitTest(event.x, event.y)
    }

    override fun onSingleTapConfirmed(event: MotionEvent?, mapView: MapView?): Boolean {
        if (event == null || mapView == null) return false
        val touched = hitTest(event, mapView)
        return if (touched) {
            onCustomMarkerClickListener?.onMarkerClick(this) ?: onMarkerClickDefault(this, mapView)
        } else {
            false
        }
    }

    interface OnCustomMarkerClickListener{
        fun onMarkerClick(marker: CustomMarker?): Boolean
    }
}
