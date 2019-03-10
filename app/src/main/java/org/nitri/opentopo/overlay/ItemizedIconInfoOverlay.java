package org.nitri.opentopo.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import org.nitri.opentopo.R;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.List;

public class ItemizedIconInfoOverlay extends ItemizedIconOverlay<OverlayItem> {

    private WayPointInfoWindow mInfoWindow;

    public ItemizedIconInfoOverlay(List<OverlayItem> pList, Drawable pDefaultMarker, OnItemGestureListener<OverlayItem> pOnItemGestureListener, Context pContext) {
        super(pList, pDefaultMarker, pOnItemGestureListener, pContext);
    }

    public ItemizedIconInfoOverlay(List<OverlayItem> pList, OnItemGestureListener<OverlayItem> pOnItemGestureListener, Context pContext) {
        super(pList, pOnItemGestureListener, pContext);
    }

    public ItemizedIconInfoOverlay(Context pContext, List<OverlayItem> pList, OnItemGestureListener<OverlayItem> pOnItemGestureListener) {
        super(pContext, pList, pOnItemGestureListener);
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);
        if (mInfoWindow != null && mInfoWindow.isOpen()) {
            mInfoWindow.draw();
        }

    }

    /**
     * Display a way point info window on the map
     *
     * @param mapView
     * @param item
     */
    public void showWayPointInfo(MapView mapView, OverlayItem item) {
        if (mInfoWindow != null && mInfoWindow.isOpen()) {
            mInfoWindow.close();
        }
        mInfoWindow = new WayPointInfoWindow(R.layout.bonuspack_bubble,
                R.id.bubble_title, R.id.bubble_description, R.id.bubble_subdescription, null, mapView);
        GeoPoint windowLocation = (GeoPoint) item.getPoint();
        mInfoWindow.open(item, windowLocation, 0, 0);

    }

    /**
     * Display a nearby item info window on the map
     *
     * @param mapView
     * @param item
     */
    public void showNearbyItemInfo(MapView mapView, OverlayItem item) {
        showWayPointInfo(mapView, item);
    }

    public void hideWayPointInfo() {
        if (mInfoWindow != null) {
            mInfoWindow.close();
        }
    }

    public WayPointInfoWindow getInfoWindow() {
        return mInfoWindow;
    }
}
