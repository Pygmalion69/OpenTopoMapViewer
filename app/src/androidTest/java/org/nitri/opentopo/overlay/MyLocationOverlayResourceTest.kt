package org.nitri.opentopo.overlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@RunWith(AndroidJUnit4::class)
class MyLocationOverlayResourceTest {

    @Test
    fun constructorLoadsDefaultIconsAtDeviceDensity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        var overlay: MyLocationNewOverlay? = null

        instrumentation.runOnMainSync {
            val mapView = MapView(context)
            overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
        }

        assertNotNull(overlay)
    }
}
