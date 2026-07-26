package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import org.osmdroid.views.Projection

internal class MarkerAnnotationRenderer(context: Context) {

    internal data class MarkerAnnotationGeometry(
        val localBounds: RectF,
        val localToScreen: Matrix,
        val screenBounds: RectF
    )

    private val fillTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val haloTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, context.resources.displayMetrics)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
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
    private val haloStrokeWidth = haloTextPaint.strokeWidth

    private val futureIconSlotWidth = 0f
    private val iconTextGap = 4f * density

    private var cachedLabelText: String? = null
    private var cachedMarkerColor: Int? = null
    private var cachedDisplayText = ""
    private var cachedTextWidth = 0f
    private var cachedTextHeight = 0f
    private var cachedFontMetrics = Paint.FontMetrics()
    private var cachedContentWidth = 0f
    private var cachedAnnotationWidth = 0f
    private var cachedAnnotationHeight = 0f

    private val cornerPoints = FloatArray(8)
    private val transformedPoints = FloatArray(8)

    private var isDrawn = false
    private var lastGeometry: MarkerAnnotationGeometry? = null

    fun clear() {
        isDrawn = false
        lastGeometry = null
    }

    internal fun getLastGeometry(): MarkerAnnotationGeometry? = lastGeometry

    private fun updateCache(text: String) {
        if (text == cachedLabelText) return
        
        cachedLabelText = text
        cachedDisplayText = TextUtils.ellipsize(text, fillTextPaint, maxLabelWidthPx, TextUtils.TruncateAt.END).toString()
        cachedTextWidth = fillTextPaint.measureText(cachedDisplayText)
        fillTextPaint.getFontMetrics(cachedFontMetrics)
        cachedTextHeight = cachedFontMetrics.descent - cachedFontMetrics.ascent

        cachedContentWidth = if (cachedTextWidth > 0 && futureIconSlotWidth > 0) {
            futureIconSlotWidth + iconTextGap + cachedTextWidth
        } else {
            futureIconSlotWidth + cachedTextWidth
        }

        cachedAnnotationWidth = cachedContentWidth + paddingHPx * 2
        cachedAnnotationHeight = cachedTextHeight + paddingVPx * 2
    }

    private fun updateColors(markerColor: Int) {
        if (markerColor == cachedMarkerColor) return
        cachedMarkerColor = markerColor
        fillTextPaint.color = markerColor
        haloTextPaint.color = haloColorFor(markerColor)
    }

    internal fun haloColorFor(markerColor: Int): Int {
        return if (ColorUtils.calculateLuminance(markerColor) > 0.65) {
            Color.argb(210, 32, 32, 32)
        } else {
            Color.argb(230, 255, 255, 255)
        }
    }

    fun draw(canvas: Canvas, pj: Projection, x: Float, y: Float, text: String, textColor: Int, iconHeight: Int, anchorV: Float, viewWidth: Int, viewHeight: Int) {
        updateCache(text)
        updateColors(textColor)
        
        val annotationBottomY = y - iconHeight * anchorV - gapPx
        val annotationLeft = x - cachedAnnotationWidth / 2
        val annotationTop = annotationBottomY - cachedAnnotationHeight
        
        backgroundRect.set(annotationLeft, annotationTop, annotationLeft + cachedAnnotationWidth, annotationTop + cachedAnnotationHeight)

        // Rotation matrix
        annotationMatrix.setRotate(-pj.orientation, x, y)
        
        // Calculate transformed bounds for clamping, including halo stroke
        val haloAllowance = haloStrokeWidth / 2f
        cornerPoints[0] = backgroundRect.left - haloAllowance; cornerPoints[1] = backgroundRect.top - haloAllowance
        cornerPoints[2] = backgroundRect.right + haloAllowance; cornerPoints[3] = backgroundRect.top - haloAllowance
        cornerPoints[4] = backgroundRect.right + haloAllowance; cornerPoints[5] = backgroundRect.bottom + haloAllowance
        cornerPoints[6] = backgroundRect.left - haloAllowance; cornerPoints[7] = backgroundRect.bottom + haloAllowance
        
        annotationMatrix.mapPoints(transformedPoints, cornerPoints)
        
        var minX = transformedPoints[0]; var maxX = transformedPoints[0]
        var minY = transformedPoints[1]; var maxY = transformedPoints[1]
        for (i in 2..7 step 2) {
            if (transformedPoints[i] < minX) minX = transformedPoints[i]
            if (transformedPoints[i] > maxX) maxX = transformedPoints[i]
            if (transformedPoints[i+1] < minY) minY = transformedPoints[i+1]
            if (transformedPoints[i+1] > maxY) maxY = transformedPoints[i+1]
        }
        
        var dx = 0f
        var dy = 0f
        
        if (minX < 0) dx = -minX
        else if (maxX > viewWidth) dx = viewWidth - maxX
        
        if (minY < 0) dy = -minY
        else if (maxY > viewHeight) dy = viewHeight - maxY
        
        // Final matrix: Translate then Rotate
        if (dx != 0f || dy != 0f) {
            annotationMatrix.postTranslate(dx, dy)
        }

        // Store geometry for testing
        val screenBounds = RectF(minX + dx, minY + dy, maxX + dx, maxY + dy)
        lastGeometry = MarkerAnnotationGeometry(
            localBounds = RectF(backgroundRect),
            localToScreen = Matrix(annotationMatrix),
            screenBounds = screenBounds
        )

        canvas.save()
        canvas.concat(annotationMatrix)

        // canvas.drawRoundRect(backgroundRect, cornerRadiusPx, cornerRadiusPx, backgroundPaint) // Removed bubble

        val textStartX = annotationLeft + paddingHPx + futureIconSlotWidth + (if (futureIconSlotWidth > 0 && cachedTextWidth > 0) iconTextGap else 0f)
        val textX = textStartX + cachedTextWidth / 2
        val textY = annotationTop + paddingVPx - cachedFontMetrics.ascent

        canvas.drawText(cachedDisplayText, textX, textY, haloTextPaint)
        canvas.drawText(cachedDisplayText, textX, textY, fillTextPaint)

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
