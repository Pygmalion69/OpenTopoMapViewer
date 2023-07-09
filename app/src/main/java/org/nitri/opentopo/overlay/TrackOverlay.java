package org.nitri.opentopo.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import androidx.core.content.ContextCompat;

import org.nitri.opentopo.R;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

public class TrackOverlay extends Overlay {

    private final Track mTrack;
    private final Context mContext;
    private final List<List<Point>> mPointsSegments = new ArrayList<>();

    private static final String TAG = TrackOverlay.class.getSimpleName();

    /**
     * Layer to display a track
     *
     * @param context
     * @param track
     * @see Track
     */
    TrackOverlay(Context context, Track track) {
        mContext = context;
        mTrack = track;
    }

    @Override
    public void draw(Canvas canvas, MapView osmv, boolean shadow) {

        //Log.d(TAG, "Zoom: " + osmv.getZoomLevelDouble());

        Paint routePaint;
        routePaint = new Paint();
        routePaint.setColor(ContextCompat.getColor(mContext, R.color.colorTrack));
        routePaint.setAntiAlias(true);
        routePaint.setAlpha(204);
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeJoin(Paint.Join.ROUND);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeWidth(12);

        if (mTrack.getTrackSegments() != null) {
            for (TrackSegment trackSegment : mTrack.getTrackSegments()) {
                Path path = new Path();
                createPointsSegments(osmv, trackSegment);
                //Log.d(TAG, "Point segments: " + mPointsSegments.size());
                if (mPointsSegments.size() > 0) {
                    for (List<Point> pointSegment : mPointsSegments) {
                        Point firstPoint = pointSegment.get(0);
                        path.moveTo(firstPoint.x, firstPoint.y);
                        Point prevPoint = firstPoint;
                        for (Point point : pointSegment) {
                            path.quadTo(prevPoint.x, prevPoint.y, point.x, point.y);
                            prevPoint = point;
                        }
                        canvas.drawPath(path, routePaint);
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
    private void createPointsSegments(MapView mapView, TrackSegment trackSegment) {
        mPointsSegments.clear();

        Point mapCenter = new Point(mapView.getWidth() / 2, mapView.getHeight() / 2);

        int offCenterLimit = (int) (mapCenter.x > mapCenter.y ? mapCenter.x * 2.5 : mapCenter.y * 2.5);
        Projection projection = mapView.getProjection();

        boolean adding = false;
        List<Point> pointsSegment = new ArrayList<>();

        for (TrackPoint trackPoint : trackSegment.getTrackPoints()) {
            GeoPoint gp = new GeoPoint(trackPoint.getLatitude(), trackPoint.getLongitude());
            Point point = projection.toPixels(gp, null);
            if (pixelDistance(mapCenter, point) < offCenterLimit) {
                if (!adding) {
                    adding = true;
                    pointsSegment.clear();
                }
                pointsSegment.add(point);
            } else {
                if (adding) {
                    mPointsSegments.add(new ArrayList<>(pointsSegment));
                    pointsSegment.clear();
                }
                adding = false;
            }
        }
        if (pointsSegment.size() > 0) {
            mPointsSegments.add(pointsSegment);
        }
    }

    private int pixelDistance(Point p1, Point p2) {
        double deltaX = p2.x - p1.x;
        double deltaY = p2.y - p1.y;
        return (int) Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }

}
