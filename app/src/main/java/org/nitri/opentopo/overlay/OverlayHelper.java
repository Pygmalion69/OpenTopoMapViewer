package org.nitri.opentopo.overlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import androidx.core.content.ContextCompat;

import org.nitri.opentopo.R;
import org.nitri.opentopo.nearby.entity.NearbyItem;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.WayPoint;

public class OverlayHelper {

    private final Context mContext;
    private final MapView mMapView;

    private ItemizedIconInfoOverlay mWayPointOverlay;
    private ItemizedIconInfoOverlay mNearbyItemOverlay;

    /**
     * Tiles Overlays
     */
    public final static int OVERLAY_NONE = 1;
    public final static int OVERLAY_HIKING = 2;
    public final static int OVERLAY_CYCLING = 3;

    private int mOverlay = OVERLAY_NONE;
    private MapTileProviderBasic mOverlayTileProvider;
    private TilesOverlay mTilesOverlay;

    private final ColorMatrix tileOverlayAlphaMatrix = new ColorMatrix(new float[]
                   {1, 0, 0, 0, 0,
                    0, 1, 0, 0, 0,
                    0, 0, 1, 0, 0,
                    0, 0, 0, 0.8f, 0}
    );
    private final ColorMatrixColorFilter tileOverlayAlphaFilter = new ColorMatrixColorFilter(tileOverlayAlphaMatrix);

    private final ItemizedIconOverlay.OnItemGestureListener<OverlayItem> mWayPointItemGestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

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

    private final ItemizedIconInfoOverlay.OnItemGestureListener<OverlayItem> mNearbyItemGestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
        @Override
        public boolean onItemSingleTapUp(int index, OverlayItem item) {
            if (mNearbyItemOverlay != null && mMapView != null) {
                mNearbyItemOverlay.showNearbyItemInfo(mMapView, item);
            }
            return true;
        }

        @Override
        public boolean onItemLongPress(int index, OverlayItem item) {
            clearNearby();
            mMapView.invalidate();
            return true;
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

        if (gpx.getTracks() != null) {
            for (Track track : gpx.getTracks()) {
                mTrackOverlay = new TrackOverlay(mContext, track);
                mMapView.getOverlays().add(0, mTrackOverlay);
            }
        }

        List<OverlayItem> wayPointItems = new ArrayList<>();

        if (gpx.getWayPoints() != null) {
            for (WayPoint wayPoint : gpx.getWayPoints()) {
                GeoPoint gp = new GeoPoint(wayPoint.getLatitude(), wayPoint.getLongitude());
                OverlayItem item = new OverlayItem(wayPoint.getName(), wayPoint.getDesc(), gp);
                wayPointItems.add(item);
            }

            mWayPointOverlay = new ItemizedIconInfoOverlay(wayPointItems, ContextCompat.getDrawable(mContext, R.drawable.ic_default_marker),
                    mWayPointItemGestureListener, mContext);

            mMapView.getOverlays().add(mWayPointOverlay);
        }

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

    public void setNearby(NearbyItem item) {
        clearNearby();
        GeoPoint geoPoint = new GeoPoint(item.getLat(), item.getLon());
        OverlayItem mapItem = new OverlayItem(item.getTitle(), item.getDescription(), geoPoint);
        mNearbyItemOverlay = new ItemizedIconInfoOverlay(new ArrayList<>(Collections.singletonList(mapItem)), ContextCompat.getDrawable(mContext, R.drawable.ic_default_marker),
                mNearbyItemGestureListener, mContext);
        mMapView.getOverlays().add(mNearbyItemOverlay);
        mMapView.invalidate();
    }

    /**
     * Remove nearby item layer
     */
    public void clearNearby() {
        if (mMapView != null && mNearbyItemOverlay != null) {
            mMapView.getOverlays().remove(mNearbyItemOverlay);
            mNearbyItemOverlay = null;
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

    public void setTilesOverlay(int overlay) {
        mOverlay = overlay;

        ITileSource overlayTiles = null;

        if (mTilesOverlay != null) {
            mMapView.getOverlays().remove(mTilesOverlay);
        }

        switch (mOverlay) {
            case OVERLAY_NONE:
                break;
            case OVERLAY_HIKING:
                overlayTiles = new XYTileSource("hiking", 1, 17, 256, ".png",
                        new String[]{
                                "https://tile.waymarkedtrails.org/hiking/"}, mContext.getString(R.string.lonvia_copy));
                break;
            case OVERLAY_CYCLING:
                overlayTiles = new XYTileSource("cycling", 1, 17, 256, ".png",
                        new String[]{
                                "https://tile.waymarkedtrails.org/cycling/"}, mContext.getString(R.string.lonvia_copy));
                break;
        }
        if (overlayTiles != null) {
            mOverlayTileProvider = new MapTileProviderBasic(mContext);
            mOverlayTileProvider.setTileSource(overlayTiles);
            mTilesOverlay = new TilesOverlay(mOverlayTileProvider, mContext);
            mTilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
            mTilesOverlay.setColorFilter(tileOverlayAlphaFilter);
            mOverlayTileProvider.getTileRequestCompleteHandlers().clear();
            mOverlayTileProvider.getTileRequestCompleteHandlers().add(mMapView.getTileRequestCompleteHandler());

            mMapView.getOverlays().add(0, mTilesOverlay);

        }
        mMapView.invalidate();
    }

    /**
     * Copyright notice for the tile overlay
     *
     * @return copyright notice or null
     */
    public String getCopyrightNotice() {
        if (mOverlayTileProvider != null && mOverlay != OVERLAY_NONE) {
            return mOverlayTileProvider.getTileSource().getCopyrightNotice();
        }
        return null;
    }

    public void destroy() {
        mTilesOverlay = null;
        mOverlayTileProvider = null;
        mWayPointOverlay = null;
    }

}
