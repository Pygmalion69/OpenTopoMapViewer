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
    fun shouldDrawLabel_respectsAllConditions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mapView = MapView(context)
        val marker = CustomMarker(mapView)
        
        marker.labelText = "Test"
        marker.labelVisible = true
        marker.minimumLabelZoom = 14.0
        
        // Mock isDisplayed via reflection since it's determined during draw
        val field = CustomMarker::class.java.superclass.getDeclaredField("mDisplayed")
        field.isAccessible = true
        field.set(marker, true)

        // All good
        assertTrue(marker.shouldDrawLabel(15.0))
        
        // Disabled setting
        marker.labelVisible = false
        assertFalse(marker.shouldDrawLabel(15.0))
        marker.labelVisible = true
        
        // Blank text
        marker.labelText = ""
        assertFalse(marker.shouldDrawLabel(15.0))
        marker.labelText = "Test"
        
        // Below zoom
        assertFalse(marker.shouldDrawLabel(13.9))
        
        // Exactly at zoom
        assertTrue(marker.shouldDrawLabel(14.0))
        
        // Not displayed (off-screen)
        field.set(marker, false)
        assertFalse(marker.shouldDrawLabel(15.0))
    }
}
