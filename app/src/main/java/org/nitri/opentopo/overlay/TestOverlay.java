package org.nitri.opentopo.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import androidx.core.content.ContextCompat;
import android.util.Log;

import org.nitri.opentopo.R;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class TestOverlay extends Overlay {

    Context mContext;

    private static final String TAG = TestOverlay.class.getSimpleName();

    public TestOverlay(Context context) {
        mContext = context;
    }

    @Override
    public void draw(Canvas canvas, MapView osmv, boolean shadow) {

        Paint routePaint;
        routePaint = new Paint();
        routePaint.setColor(ContextCompat.getColor(mContext, R.color.colorTrack));
        routePaint.setAntiAlias(true);
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeJoin(Paint.Join.ROUND);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeWidth(12);

        Projection projection = osmv.getProjection();

        //TEST:
        GeoPoint gp1 = new GeoPoint(51.7868500,6.0580036);
        //GeoPoint gp2 = new GeoPoint(51.7868766,6.0587331);
        GeoPoint gp2 = new GeoPoint(51.791469, 6.114177);
        Point point1 = projection.toPixels(gp1, null);
        Point point2 = projection.toPixels(gp2, null);
        Path testPath = new Path();
        testPath.moveTo(point1.x, point1.y);
        testPath.lineTo(point2.x, point2.y);

        //canvas.clipPath(testPath);

        canvas.drawPath(testPath, routePaint);

        Log.d(TAG, "Pixel distance: " + pixelDistance(point1, point2));


    }

    private int pixelDistance(Point p1, Point p2) {
        double deltaX = p2.x - p1.x;
        double deltaY = p2.y - p1.y;
        return (int) Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }
}
