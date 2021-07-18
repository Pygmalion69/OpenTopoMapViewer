package org.nitri.opentopo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import io.ticofab.androidgpxparser.parser.domain.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;
import io.ticofab.androidgpxparser.parser.domain.WayPoint;

public class Util {

    static final int NO_ELEVATION_VALUE = -99999;

    /**
     * Distance between points
     *
     * @param point1
     * @param point2
     * @return meters
     */
    public static double distance(Point point1, Point point2) {
        double lat1 = point1.getLatitude();
        double lon1 = point1.getLongitude();
        double lat2 = point2.getLatitude();
        double lon2 = point2.getLongitude();
        return distance(lat1, lon1, lat2, lon2);
    }

    /**
     * Distance between lat/lon pairs
     *
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return meters
     */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2)
            return 0;
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1609.344;
        return dist;
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    /**
     * Get GPX bounds
     *
     * @param gpx Gpx
     * @return BoundingBox
     */
    public static BoundingBox area(Gpx gpx) {
        return area(getAllTrackGeoPoints(gpx));
    }

    /**
     * Get geo points bounds
     *
     * @param points List<GeoPoint>
     * @return BoundingBox
     */
    public static BoundingBox area(List<GeoPoint> points) {

        double north = 0;
        double south = 0;
        double west = 0;
        double east = 0;

        for (int i = 0; i < points.size(); i++) {
            if (points.get(i) == null) continue;
            double lat = points.get(i).getLatitude();
            double lon = points.get(i).getLongitude();
            if ((i == 0) || (lat > north)) north = lat;
            if ((i == 0) || (lat < south)) south = lat;
            if ((i == 0) || (lon < west)) west = lon;
            if ((i == 0) || (lon > east)) east = lon;
        }
        return new BoundingBox(north, east, south, west);
    }

    private static List<GeoPoint> getAllTrackGeoPoints(Gpx gpx) {
        List<GeoPoint> geoPoints = new ArrayList<>();
        if (gpx != null && gpx.getTracks() != null) {
            for (Track track: gpx.getTracks()) {
                if (track.getTrackSegments() != null) {
                    for (TrackSegment segment: track.getTrackSegments()) {
                        if (segment.getTrackPoints() != null) {
                            for (TrackPoint trackPoint: segment.getTrackPoints()){
                                geoPoints.add(new GeoPoint(trackPoint.getLatitude(), trackPoint.getLongitude()));
                            }
                        }
                    }
                }
            }
        }
        return geoPoints;
    }

    /**
     * Get way point types (categories) from GPX
     *
     * @param gpx
     * @param defaultType
     * @return
     */
    public static List<String> getWayPointTypes(Gpx gpx, String defaultType) {
        List<String> types = new ArrayList<>();
        if (gpx.getWayPoints() != null) {
            for (WayPoint wayPoint: gpx.getWayPoints()) {
                String type = defaultType;
                if (!TextUtils.isEmpty(wayPoint.getType()))
                    type = wayPoint.getType();
                if (!types.contains(type))
                    types.add(type);
            }
        }
        Collections.sort(types);
        return types;
    }

    /**
     * Get a list of way points by type (categpry)
     *
     * @param gpx
     * @param type
     * @return
     */
    public static List<WayPoint> getWayPointsByType(Gpx gpx, String type) {
        List<WayPoint> wayPoints = new ArrayList<>();
        if (gpx.getWayPoints() != null) {
            for (WayPoint wayPoint: gpx.getWayPoints()) {
                if (!TextUtils.isEmpty(wayPoint.getType()) && wayPoint.getType().equals(type))
                    wayPoints.add(wayPoint);
                else if (TextUtils.isEmpty(wayPoint.getType()) && TextUtils.isEmpty(type))
                    wayPoints.add(wayPoint);
            }
        }
        return wayPoints;
    }

    private static TypedValue resolveThemeAttr(Context context, @AttrRes int attrRes) {
        Resources.Theme theme = context.getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attrRes, typedValue, true);
        return typedValue;
    }

    /**
     * Get color integer by attribute
     *
     * @param context
     * @param colorAttr
     * @return
     */
    @ColorInt
    public static int resolveColorAttr(Context context, @AttrRes int colorAttr) {
        TypedValue resolvedAttr = resolveThemeAttr(context, colorAttr);
        // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
        int colorRes = resolvedAttr.resourceId != 0 ? resolvedAttr.resourceId : resolvedAttr.data;
        return ContextCompat.getColor(context, colorRes);
    }

    /**
     * Spanned text from HTML (compat)
     *
     * @param source
     * @return
     */
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    /**
     * Antenna altitude above mean sea level
     *
     * @param nmea NMEA
     * @return antenna altitude
     */
    public static double elevationFromNmea(String nmea) {
        if (!TextUtils.isEmpty(nmea) && nmea.startsWith("$GPGGA")) {
            String[] tokens = nmea.split(",");
            try {
                String elevation = tokens[9];
                if (!TextUtils.isEmpty(elevation)) {
                    return Double.parseDouble(elevation);
                }
            } catch (Exception ex) {
                Log.e("NMEA", "elevationFromNmea: "
                        + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return NO_ELEVATION_VALUE;
    }

    /**
     * Bitmap from vector drawable
     *
     * @param context
     * @param drawableId
     * @return
     */
    public static Bitmap getBitmapFromDrawable(Context context, @DrawableRes int drawableId, int alpha) {
        Drawable drawable = AppCompatResources.getDrawable(context, drawableId);
        drawable.setAlpha(alpha);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawableCompat) {
            return createBitmap(drawable);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable instanceof VectorDrawable) {
            return createBitmap(drawable);
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    private static Bitmap createBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
