package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.util.Log
import androidx.core.content.ContextCompat
import io.ticofab.androidgpxparser.parser.domain.Track
import io.ticofab.androidgpxparser.parser.domain.TrackSegment
import org.nitri.opentopo.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Layer to display a track
 *
 * @param context
 * @param track
 * @see Track
 */
class TrackOverlay(
    private val context: Context,
    private val track: Track
) : Overlay() {

    private val mPointsSegments: MutableList<List<Point>> = ArrayList()
    override fun draw(canvas: Canvas, osmv: MapView, shadow: Boolean) {

        //Log.d(TAG, "Zoom: " + osmv.getZoomLevelDouble());
        val routePaint = Paint()
        routePaint.color = ContextCompat.getColor(context, R.color.colorTrack)
        routePaint.isAntiAlias = true
        routePaint.alpha = 204
        routePaint.style = Paint.Style.STROKE
        routePaint.strokeJoin = Paint.Join.ROUND
        routePaint.strokeCap = Paint.Cap.ROUND
        routePaint.strokeWidth = 12f
        if (track.trackSegments != null) {
            for (trackSegment in track.trackSegments) {
                val path = Path()
                createPointsSegments(osmv, trackSegment)
                //Log.d(TAG, "Points segments: " + mPointsSegments.size);
                if (mPointsSegments.isNotEmpty()) {
                    for (pointSegment in mPointsSegments) {
                        val firstPoint = pointSegment[0]
                        path.moveTo(firstPoint.x.toFloat(), firstPoint.y.toFloat())
                        var prevPoint = firstPoint
                        for (point in pointSegment) {
                            path.quadTo(
                                prevPoint.x.toFloat(),
                                prevPoint.y.toFloat(),
                                point.x.toFloat(),
                                point.y.toFloat()
                            )
                            prevPoint = point
                        }
                        canvas.drawPath(path, routePaint)
                    }
                }
            }
        }
    }

    /**
     * Since off-canvas drawing may discard the rendering of the entire track segment, cut the
     * track segment into points segments that need to be rendered.
     *
     * @param mapView
     * @param trackSegment
     */
    private fun createPointsSegments(mapView: MapView, trackSegment: TrackSegment) {
        mPointsSegments.clear()
        val mapCenter = Point(mapView.width / 2, mapView.height / 2)
        val offCenterLimit =
            (if (mapCenter.x > mapCenter.y) mapCenter.x * 2.5 else mapCenter.y * 2.5).toInt()
        val projection = mapView.projection
        var adding = false
        val pointsSegment: MutableList<Point> = ArrayList()
        for (trackPoint in trackSegment.trackPoints) {
            val gp = GeoPoint(trackPoint.latitude, trackPoint.longitude)
            val point = projection.toPixels(gp, null)
            if (pixelDistance(mapCenter, point) < offCenterLimit) {
                if (!adding) {
                    adding = true
                    pointsSegment.clear()
                }

                pointsSegment.add(point)
            } else {
                if (adding) {
                    mPointsSegments.add(ArrayList(pointsSegment))
                    pointsSegment.clear()
                }
                adding = false
            }
        }
        if (pointsSegment.isNotEmpty()) {
            mPointsSegments.add(pointsSegment)
        }

        // fallback
        if (mPointsSegments.isEmpty() && trackSegment.trackPoints.isNotEmpty()) {
            val fallbackPoints = trackSegment.trackPoints.map {
                projection.toPixels(GeoPoint(it.latitude, it.longitude), null)
            }
            mPointsSegments.add(fallbackPoints)
        }
    }

    private fun pixelDistance(p1: Point, p2: Point): Int {
        val deltaX = (p2.x - p1.x).toDouble()
        val deltaY = (p2.y - p1.y).toDouble()
        return sqrt(deltaX.pow(2.0) + deltaY.pow(2.0)).toInt()
    }

    companion object {
        private val TAG = TrackOverlay::class.java.simpleName
    }
}
