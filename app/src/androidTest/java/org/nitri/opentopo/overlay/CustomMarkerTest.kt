package org.nitri.opentopo.overlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView

@RunWith(AndroidJUnit4::class)
class CustomMarkerTest {

    @Test
    fun customMarker_initialState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mapView = MapView(context)
        val marker = CustomMarker(mapView)
        
        assertEquals("", marker.labelText)
        assertFalse(marker.labelVisible)
        assertEquals(14.0, marker.minimumLabelZoom, 0.0)
    }

    @Test
    fun customMarker_labelVisibilityToggle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mapView = MapView(context)
        val marker = CustomMarker(mapView)
        
        marker.labelVisible = true
        assertTrue(marker.labelVisible)
        
        marker.labelVisible = false
        assertFalse(marker.labelVisible)
    }
}
