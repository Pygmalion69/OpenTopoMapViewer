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

    private val annotationRenderer = AnnotationRenderer()

    var onMarkerInfoEditClickListener: MarkerInfoWindow.OnMarkerInfoEditClickListener? = null
    var onMarkerWaypointClickListener: MarkerInfoWindow.OnMarkerWaypointClickListener? = null
    var onCustomMarkerClickListener: OnCustomMarkerClickListener? = null

    override fun draw(canvas: Canvas, pj: Projection) {
        super.draw(canvas, pj)

        if (!labelVisible || labelText.isBlank() || (pj.zoomLevel < minimumLabelZoom)) {
            return
        }

        val x = mPositionPixels.x.toFloat()
        val y = mPositionPixels.y.toFloat()

        annotationRenderer.draw(canvas, pj, x, y, labelText, mIcon.intrinsicHeight, mAnchorV)
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

    private inner class AnnotationRenderer {

        private val labelTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, mapView.resources.displayMetrics)
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 220
        }

        private val backgroundRect = RectF()
        private val annotationMatrix = Matrix()
        private val invertedMatrix = Matrix()
        private val touchPoint = floatArrayOf(0f, 0f)

        private val density = mapView.resources.displayMetrics.density
        private val maxLabelWidthPx = 120f * density
        private val paddingHPx = 4f * density
        private val paddingVPx = 2f * density
        private val gapPx = 2f * density
        private val cornerRadiusPx = 3f * density

        private val futureIconSlotWidth = 0f
        private val iconTextGap = 4f * density

        fun draw(canvas: Canvas, pj: Projection, x: Float, y: Float, text: String, iconHeight: Int, anchorV: Float) {
            val annotationBottomY = y - iconHeight * anchorV - gapPx

            val ellipsizedText = TextUtils.ellipsize(text, labelTextPaint, maxLabelWidthPx, TextUtils.TruncateAt.END)
            val textWidth = labelTextPaint.measureText(ellipsizedText.toString())
            val fm = labelTextPaint.fontMetrics
            val textHeight = fm.descent - fm.ascent

            val contentWidth = if (textWidth > 0 && futureIconSlotWidth > 0) {
                futureIconSlotWidth + iconTextGap + textWidth
            } else {
                futureIconSlotWidth + textWidth
            }

            val annotationWidth = contentWidth + paddingHPx * 2
            val annotationHeight = textHeight + paddingVPx * 2

            var annotationLeft = x - annotationWidth / 2
            val annotationTop = annotationBottomY - annotationHeight

            // Viewport adjustment
            val mapViewWidth = mapView.width
            if (annotationLeft < 0) {
                annotationLeft = 0f
            } else if (annotationLeft + annotationWidth > mapViewWidth) {
                annotationLeft = mapViewWidth - annotationWidth
            }

            backgroundRect.set(annotationLeft, annotationTop, annotationLeft + annotationWidth, annotationTop + annotationHeight)

            canvas.save()
            // Stay horizontal: counteract map orientation
            canvas.rotate(-pj.orientation, x, y)
            
            // Rebuild matrix for hit testing instead of using deprecated canvas.matrix
            annotationMatrix.setRotate(-pj.orientation, x, y)

            canvas.drawRoundRect(backgroundRect, cornerRadiusPx, cornerRadiusPx, backgroundPaint)

            val textStartX = annotationLeft + paddingHPx + futureIconSlotWidth + (if (futureIconSlotWidth > 0 && textWidth > 0) iconTextGap else 0f)
            val textX = textStartX + textWidth / 2
            val textY = annotationTop + paddingVPx - fm.ascent
            canvas.drawText(ellipsizedText.toString(), textX, textY, labelTextPaint)

            canvas.restore()
        }

        fun hitTest(x: Float, y: Float): Boolean {
            if (annotationMatrix.invert(invertedMatrix)) {
                touchPoint[0] = x
                touchPoint[1] = y
                invertedMatrix.mapPoints(touchPoint)
                return backgroundRect.contains(touchPoint[0], touchPoint[1])
            }
            return false
        }
    }

    interface OnCustomMarkerClickListener{
        fun onMarkerClick(marker: CustomMarker?): Boolean
    }
}
