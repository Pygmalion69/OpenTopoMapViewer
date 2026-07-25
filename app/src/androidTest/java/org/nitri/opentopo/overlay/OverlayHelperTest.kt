package org.nitri.opentopo.overlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.model.MarkerModel
import org.osmdroid.views.MapView

@RunWith(AndroidJUnit4::class)
class OverlayHelperTest {

    @Test
    fun setMarkers_clearsMapMarkersCollection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mapView = MapView(context)
        val overlayHelper = OverlayHelper(context, mapView)
        
        val listener = object : OverlayHelper.MarkerInteractionListener {
            override fun onMarkerMoved(markerModel: MarkerModel) {}
            override fun onMarkerClicked(markerModel: MarkerModel) {}
            override fun onMarkerDelete(markerModel: MarkerModel) {}
            override fun onMarkerUpdate(markerModel: MarkerModel) {}
            override fun onMarkerWaypointsChanged() {}
        }
        
        overlayHelper.setMarkers(listOf(MarkerModel(seq = 1, latitude = 0.0, longitude = 0.0, name = "M1", description = "")), listener)
        
        val field = OverlayHelper::class.java.getDeclaredField("mapMarkers")
        field.isAccessible = true
        val mapMarkers = field.get(overlayHelper) as List<*>
        assertEquals(1, mapMarkers.size)
        
        overlayHelper.setMarkers(listOf(MarkerModel(seq = 2, latitude = 1.0, longitude = 1.0, name = "M2", description = "")), listener)
        assertEquals(1, mapMarkers.size)
    }

    @Test
    fun updateMarkerLabelVisibility_updatesAllMarkers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mapView = MapView(context)
        val overlayHelper = OverlayHelper(context, mapView)
        
        val listener = object : OverlayHelper.MarkerInteractionListener {
            override fun onMarkerMoved(markerModel: MarkerModel) {}
            override fun onMarkerClicked(markerModel: MarkerModel) {}
            override fun onMarkerDelete(markerModel: MarkerModel) {}
            override fun onMarkerUpdate(markerModel: MarkerModel) {}
            override fun onMarkerWaypointsChanged() {}
        }
        
        overlayHelper.setMarkers(listOf(
            MarkerModel(seq = 1, latitude = 0.0, longitude = 0.0, name = "M1", description = ""),
            MarkerModel(seq = 2, latitude = 1.0, longitude = 1.0, name = "M2", description = "")
        ), listener)

        overlayHelper.updateMarkerLabelVisibility(true)
        
        val field = OverlayHelper::class.java.getDeclaredField("mapMarkers")
        field.isAccessible = true
        val mapMarkers = field.get(overlayHelper) as List<CustomMarker>
        
        assertTrue(mapMarkers.all { it.labelVisible })
        
        overlayHelper.updateMarkerLabelVisibility(false)
        assertTrue(mapMarkers.all { !it.labelVisible })
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
