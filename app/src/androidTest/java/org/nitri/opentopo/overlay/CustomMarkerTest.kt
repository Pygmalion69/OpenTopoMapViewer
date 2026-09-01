package org.nitri.opentopo.overlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView

@RunWith(AndroidJUnit4::class)
class CustomMarkerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun customMarker_initialState() {
        // We only need to check initial property values, which don't require superclass methods
        // But we still need to instantiate it. Let's use a real MapView on the UI thread.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var marker: CustomMarker? = null
        instrumentation.runOnMainSync {
            val mapView = MapView(context)
            marker = CustomMarker(mapView)
        }
        
        marker?.let {
            assertEquals("", it.labelText)
            assertFalse(it.labelVisible)
            assertEquals(DEFAULT_MARKER_LABEL_MIN_ZOOM, it.minimumLabelZoom, 0.0)
        } ?: fail("Marker was not created")
    }

    @Test
    fun shouldDrawMarkerLabel_respectsAllConditions() {
        // Pure function test - no MapView or reflection needed
        
        // All good
        assertTrue(shouldDrawMarkerLabel(
            labelVisible = true, labelText = "Test", zoom = 15.0, minimumLabelZoom = 14.0, isDisplayed = true
        ))
        
        // Disabled setting
        assertFalse(shouldDrawMarkerLabel(
            labelVisible = false, labelText = "Test", zoom = 15.0, minimumLabelZoom = 14.0, isDisplayed = true
        ))
        
        // Blank text
        assertFalse(shouldDrawMarkerLabel(
            labelVisible = true, labelText = "", zoom = 15.0, minimumLabelZoom = 14.0, isDisplayed = true
        ))
        
        // Whitespace text
        assertFalse(shouldDrawMarkerLabel(
            labelVisible = true, labelText = "  ", zoom = 15.0, minimumLabelZoom = 14.0, isDisplayed = true
        ))
        
        // Below zoom
        assertFalse(shouldDrawMarkerLabel(
            labelVisible = true, labelText = "Test", zoom = 13.9, minimumLabelZoom = 14.0, isDisplayed = true
        ))
        
        // Exactly at zoom
        assertTrue(shouldDrawMarkerLabel(
            labelVisible = true, labelText = "Test", zoom = 14.0, minimumLabelZoom = 14.0, isDisplayed = true
        ))
        
        // Not displayed (off-screen)
        assertFalse(shouldDrawMarkerLabel(
            labelVisible = true, labelText = "Test", zoom = 15.0, minimumLabelZoom = 14.0, isDisplayed = false
        ))
    }
}
