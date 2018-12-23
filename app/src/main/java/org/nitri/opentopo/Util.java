package org.nitri.opentopo;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

public class Util {

    public static BoundingBox area(Gpx gpx) {
        return area(getAllTrackGeoPoints(gpx));
    }

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
