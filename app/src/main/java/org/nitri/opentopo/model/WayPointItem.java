package org.nitri.opentopo.model;

import io.ticofab.androidgpxparser.parser.domain.WayPoint;

public class WayPointItem implements WayPointListItem {

    private WayPoint wayPoint;

    public WayPointItem(WayPoint wayPoint) {
        this.wayPoint = wayPoint;
    }

    public WayPoint getWayPoint() {
        return wayPoint;
    }

    public void setWayPoint(WayPoint wayPoint) {
        this.wayPoint = wayPoint;
    }

    @Override
    public int getListItemType() {
        return WAY_POINT;
    }
}
