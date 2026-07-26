package org.nitri.opentopo.overlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.model.MarkerModel
import org.osmdroid.views.MapView

@RunWith(AndroidJUnit4::class)
class OverlayHelperTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun runOnMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    @Test
    fun setMarkers_clearsPreviousMarkersFromCollection() {
        runOnMain {
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
            @Suppress("UNCHECKED_CAST")
            val mapMarkers = field.get(overlayHelper) as List<CustomMarker>
            assertEquals(1, mapMarkers.size)
            
            overlayHelper.setMarkers(listOf(MarkerModel(seq = 2, latitude = 1.0, longitude = 1.0, name = "M2", description = "")), listener)
            assertEquals("Collection size should remain 1 after re-setting markers", 1, mapMarkers.size)
            assertEquals("M2", mapMarkers[0].labelText)
        }
    }

    @Test
    fun updateMarkerLabelVisibility_appliesToAllActiveMarkers() {
        runOnMain {
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
            @Suppress("UNCHECKED_CAST")
            val mapMarkers = field.get(overlayHelper) as List<CustomMarker>
            
            assertTrue("All markers should have labels enabled", mapMarkers.all { it.labelVisible })
            
            overlayHelper.updateMarkerLabelVisibility(false)
            assertTrue("All markers should have labels disabled", mapMarkers.all { !it.labelVisible })
        }
    }
}
