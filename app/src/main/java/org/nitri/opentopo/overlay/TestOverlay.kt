package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.util.Log
import androidx.core.content.ContextCompat
import org.nitri.opentopo.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.pow
import kotlin.math.sqrt

class TestOverlay(private var mContext: Context) : Overlay() {
    override fun draw(canvas: Canvas, osmv: MapView, shadow: Boolean) {
        val routePaint = Paint()
        routePaint.color = ContextCompat.getColor(mContext, R.color.colorTrack)
        routePaint.isAntiAlias = true
        routePaint.style = Paint.Style.STROKE
        routePaint.strokeJoin = Paint.Join.ROUND
        routePaint.strokeCap = Paint.Cap.ROUND
        routePaint.strokeWidth = 12f
        val projection = osmv.getProjection()

        //TEST:
        val gp1 = GeoPoint(51.7868500, 6.0580036)
        //GeoPoint gp2 = new GeoPoint(51.7868766,6.0587331);
        val gp2 = GeoPoint(51.791469, 6.114177)
        val point1 = projection.toPixels(gp1, null)
        val point2 = projection.toPixels(gp2, null)
        val testPath = Path()
        testPath.moveTo(point1.x.toFloat(), point1.y.toFloat())
        testPath.lineTo(point2.x.toFloat(), point2.y.toFloat())

        //canvas.clipPath(testPath);
        canvas.drawPath(testPath, routePaint)
        Log.d(TAG, "Pixel distance: " + pixelDistance(point1, point2))
    }

    private fun pixelDistance(p1: Point, p2: Point): Int {
        val deltaX = (p2.x - p1.x).toDouble()
        val deltaY = (p2.y - p1.y).toDouble()
        return sqrt(deltaX.pow(2.0) + deltaY.pow(2.0)).toInt()
    }

    companion object {
        private val TAG = TestOverlay::class.java.simpleName
    }
}
