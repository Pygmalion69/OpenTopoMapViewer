package org.nitri.opentopo.overlay;

import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

public class GestureOverlay extends Overlay {

    GestureCallback gestureCallback;

    public GestureOverlay(GestureCallback gestureCallback) {
        this.gestureCallback = gestureCallback;
    }

    @Override
    public boolean onFling(MotionEvent pEvent1, MotionEvent pEvent2, float pVelocityX, float pVelocityY, MapView pMapView) {
       gestureCallback.onUserMapInteraction();
        return super.onFling(pEvent1, pEvent2, pVelocityX, pVelocityY, pMapView);
    }

    @Override
    public boolean onScroll(MotionEvent pEvent1, MotionEvent pEvent2, float pDistanceX, float pDistanceY, MapView pMapView) {
        gestureCallback.onUserMapInteraction();
        return super.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView);
    }

    public interface GestureCallback {
        void onUserMapInteraction();
    }
}
