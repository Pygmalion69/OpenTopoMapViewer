package org.nitri.opentopo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint

@RunWith(AndroidJUnit4::class)
class MapStartupLayoutTest {
    @Test fun controllerCenterIsDeferredUntilFirstLayout() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val map = MapView(InstrumentationRegistry.getInstrumentation().targetContext)
            try {
                map.controller.setZoom(15.0)
                map.controller.setCenter(GeoPoint(51.78962, 6.14120))
                assertFalse(map.isLayoutOccurred)
                assertEquals(0.0, map.mapCenter.latitude, 0.0001)
                assertEquals(0.0, map.mapCenter.longitude, 0.0001)
                map.layout(0, 0, 800, 800)
                assertEquals(51.78962, map.mapCenter.latitude, 0.0001)
            } finally { map.onDetach() }
        }
    }
    @Test fun expectedCenterIsAvailableBeforeLayoutAndSurvivesLayout() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val map = MapView(InstrumentationRegistry.getInstrumentation().targetContext)
            try {
                map.controller.setZoom(15.0)
                map.setExpectedCenter(GeoPoint(51.78962, 6.14120))
                assertEquals(51.78962, map.mapCenter.latitude, 0.0001)
                assertEquals(6.14120, map.mapCenter.longitude, 0.0001)
                map.layout(0, 0, 800, 800)
                assertEquals(51.78962, map.mapCenter.latitude, 0.0001)
                assertEquals(6.14120, map.mapCenter.longitude, 0.0001)
            } finally { map.onDetach() }
        }
    }

}
