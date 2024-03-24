package org.nitri.opentopo.overlay

import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class GestureOverlay(private var gestureCallback: GestureCallback) : Overlay() {
    override fun onFling(
        pEvent1: MotionEvent,
        pEvent2: MotionEvent,
        pVelocityX: Float,
        pVelocityY: Float,
        pMapView: MapView
    ): Boolean {
        gestureCallback.onUserMapInteraction()
        return super.onFling(pEvent1, pEvent2, pVelocityX, pVelocityY, pMapView)
    }

    override fun onScroll(
        pEvent1: MotionEvent,
        pEvent2: MotionEvent,
        pDistanceX: Float,
        pDistanceY: Float,
        pMapView: MapView
    ): Boolean {
        gestureCallback.onUserMapInteraction()
        return super.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView)
    }

    interface GestureCallback {
        fun onUserMapInteraction()
    }
}
