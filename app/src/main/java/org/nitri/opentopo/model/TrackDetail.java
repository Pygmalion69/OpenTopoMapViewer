package org.nitri.opentopo.model;

import org.nitri.opentopo.domain.DistancePoint;

import java.util.Collections;
import java.util.List;

public class TrackDetail {

    private String name;
    private List<DistancePoint> distancePoints;

    public TrackDetail(String name, List<DistancePoint> distancePoints) {
        this.name = name;
        this.distancePoints = Collections.unmodifiableList(distancePoints);
    }

    public String getName() {
        return name;
    }

    public List<DistancePoint> getDistancePoints() {
        return distancePoints;
    }
}
