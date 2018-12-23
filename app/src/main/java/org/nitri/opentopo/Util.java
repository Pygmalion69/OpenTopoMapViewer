package org.nitri.opentopo;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import io.ticofab.androidgpxparser.parser.domain.Point;
import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

public class Util {

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
        return (dist);
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

}
