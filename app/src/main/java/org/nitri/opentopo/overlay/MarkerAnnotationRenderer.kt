package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import org.osmdroid.views.Projection

internal class MarkerAnnotationRenderer(context: Context) {

    private val labelTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
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

    private val density = context.resources.displayMetrics.density
    private val maxLabelWidthPx = 120f * density
    private val paddingHPx = 4f * density
    private val paddingVPx = 2f * density
    private val gapPx = 2f * density
    private val cornerRadiusPx = 3f * density

    private val futureIconSlotWidth = 0f
    private val iconTextGap = 4f * density

    private var cachedLabelText: String? = null
    private var cachedEllipsizedText: CharSequence? = null
    private var cachedTextWidth = 0f
    private var cachedTextHeight = 0f
    private var cachedFontMetrics = Paint.FontMetrics()
    private var cachedContentWidth = 0f
    private var cachedAnnotationWidth = 0f
    private var cachedAnnotationHeight = 0f

    private var isDrawn = false

    fun clear() {
        isDrawn = false
    }

    private fun updateCache(text: String) {
        if (text == cachedLabelText) return
        
        cachedLabelText = text
        cachedEllipsizedText = TextUtils.ellipsize(text, labelTextPaint, maxLabelWidthPx, TextUtils.TruncateAt.END)
        cachedTextWidth = labelTextPaint.measureText(cachedEllipsizedText.toString())
        labelTextPaint.getFontMetrics(cachedFontMetrics)
        cachedTextHeight = cachedFontMetrics.descent - cachedFontMetrics.ascent

        cachedContentWidth = if (cachedTextWidth > 0 && futureIconSlotWidth > 0) {
            futureIconSlotWidth + iconTextGap + cachedTextWidth
        } else {
            futureIconSlotWidth + cachedTextWidth
        }

        cachedAnnotationWidth = cachedContentWidth + paddingHPx * 2
        cachedAnnotationHeight = cachedTextHeight + paddingVPx * 2
    }

    fun draw(canvas: Canvas, pj: Projection, x: Float, y: Float, text: String, iconHeight: Int, anchorV: Float, viewWidth: Int, viewHeight: Int) {
        updateCache(text)
        
        val annotationBottomY = y - iconHeight * anchorV - gapPx

        var annotationLeft = x - cachedAnnotationWidth / 2
        var annotationTop = annotationBottomY - cachedAnnotationHeight

        // Horizontal clamping
        if (annotationLeft < 0) {
            annotationLeft = 0f
        } else if (annotationLeft + cachedAnnotationWidth > viewWidth) {
            annotationLeft = viewWidth - cachedAnnotationWidth
        }

        // Vertical clamping
        if (annotationTop < 0) {
            annotationTop = 0f
        } else if (annotationTop + cachedAnnotationHeight > viewHeight) {
            annotationTop = viewHeight - cachedAnnotationHeight
        }

        backgroundRect.set(annotationLeft, annotationTop, annotationLeft + cachedAnnotationWidth, annotationTop + cachedAnnotationHeight)

        canvas.save()
        // Stay horizontal: counteract map orientation
        canvas.rotate(-pj.orientation, x, y)
        
        // Rebuild matrix for hit testing
        annotationMatrix.setRotate(-pj.orientation, x, y)

        canvas.drawRoundRect(backgroundRect, cornerRadiusPx, cornerRadiusPx, backgroundPaint)

        val textStartX = annotationLeft + paddingHPx + futureIconSlotWidth + (if (futureIconSlotWidth > 0 && cachedTextWidth > 0) iconTextGap else 0f)
        val textX = textStartX + cachedTextWidth / 2
        val textY = annotationTop + paddingVPx - cachedFontMetrics.ascent
        canvas.drawText(cachedEllipsizedText.toString(), textX, textY, labelTextPaint)

        canvas.restore()
        isDrawn = true
    }

    fun hitTest(x: Float, y: Float): Boolean {
        if (!isDrawn) return false
        if (annotationMatrix.invert(invertedMatrix)) {
            touchPoint[0] = x
            touchPoint[1] = y
            invertedMatrix.mapPoints(touchPoint)
            return backgroundRect.contains(touchPoint[0], touchPoint[1])
        }
        return false
    }
}
