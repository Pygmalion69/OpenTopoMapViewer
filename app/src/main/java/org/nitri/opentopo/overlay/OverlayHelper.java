package org.nitri.opentopo.overlay;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import org.nitri.opentopo.R;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.WayPoint;

public class OverlayHelper {

    Context mContext;
    MapView mMapView;

    private ItemizedIconInfoOverlay mWayPointOverlay;


    private ItemizedIconOverlay.OnItemGestureListener<OverlayItem> mWayPointItemGestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

        @Override
        public boolean onItemSingleTapUp(int index, OverlayItem item) {
            if (mWayPointOverlay != null && mMapView != null) {
                mWayPointOverlay.showWayPointInfo(mMapView, item);
            }
            return true;
        }

        @Override
        public boolean onItemLongPress(int index, OverlayItem item) {
            return false;
        }
    };

    public BasicInfoWindow getInfoWindow() {
        if (mWayPointOverlay != null) {
            return mWayPointOverlay.getInfoWindow();
        }
        return null;
    }

    private TrackOverlay mTrackOverlay;

    public OverlayHelper(Context context, MapView mapView) {
        this.mContext = context;
        this.mMapView = mapView;
    }

    /**
     * Add GPX as an overlay
     *
     * @param gpx
     * @see Gpx
     */
    public void setGpx(Gpx gpx) {

        clearGpx();

        for (Track track : gpx.getTracks()) {
            mTrackOverlay = new TrackOverlay(mContext, track);
            mMapView.getOverlays().add(mTrackOverlay);
        }

        List<OverlayItem> wayPointItems = new ArrayList<>();

        for (WayPoint wayPoint : gpx.getWayPoints()) {
            GeoPoint gp = new GeoPoint(wayPoint.getLatitude(), wayPoint.getLongitude());
            OverlayItem item = new OverlayItem(wayPoint.getName(), wayPoint.getDesc(), gp);
            wayPointItems.add(item);
        }

        mWayPointOverlay = new ItemizedIconInfoOverlay(wayPointItems, ContextCompat.getDrawable(mContext, R.drawable.ic_default_marker),
                mWayPointItemGestureListener, mContext);

        mMapView.getOverlays().add(mWayPointOverlay);

        mMapView.invalidate();

    }

    /**
     * Remove GPX layer
     */
    public void clearGpx() {
        if (mMapView != null) {
            if (mTrackOverlay != null) {
                mMapView.getOverlays().remove(mTrackOverlay);
                mTrackOverlay = null;
            }
            if (mWayPointOverlay != null) {
                mMapView.getOverlays().remove(mWayPointOverlay);
                mWayPointOverlay = null;
            }
        }
    }

    /**
     * Returns whether a GPX has been added
     *
     * @return GPX layer present
     */
    public boolean hasGpx() {
        return mTrackOverlay != null || mWayPointOverlay != null;
    }

}
