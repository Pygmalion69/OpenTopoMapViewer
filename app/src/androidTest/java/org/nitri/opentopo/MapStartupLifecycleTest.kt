package org.nitri.opentopo

import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.viewmodel.LocationViewModel
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/** Controlled no-provider startup; permission UI and real GPS delivery remain device checks. */
@RunWith(AndroidJUnit4::class)
class MapStartupLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs get() = PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext)
    private val keys = listOf("latitude", "longitude", "zoom", "pending_first_fix", "follow")
    private var original: Map<String, *> = emptyMap<String, Any>()

    @Before fun prepare() {
        original = prefs.all.filterKeys { it in keys }
        for (permission in listOf("ACCESS_COARSE_LOCATION", "ACCESS_FINE_LOCATION")) {
            instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${instrumentation.targetContext.packageName} android.permission.$permission"
            ).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        }
    }

    @After fun restorePreferences() {
        prefs.edit().apply {
            keys.forEach { remove(it) }
            original.forEach { (key, value) ->
                when (value) {
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }.commit()
    }

    private fun field(fragment: MapFragment, name: String) =
        MapFragment::class.java.getDeclaredField(name).apply { isAccessible = true }
    private fun call(fragment: MapFragment, name: String) =
        MapFragment::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(fragment)
    private fun fragment(activity: MainActivity) =
        activity.supportFragmentManager.findFragmentById(R.id.map_container) as MapFragment
    private fun map(fragment: MapFragment) = field(fragment, "mapView").get(fragment) as MapView
    private fun pending(fragment: MapFragment) = field(fragment, "awaitingFirstFix").getBoolean(fragment)
    private fun fix(fragment: MapFragment, lat: Double = 48.0, lon: Double = 8.0) {
        fragment.onLocationChanged(Location("test").apply { latitude = lat; longitude = lon })
    }
    private fun resetStartup(fragment: MapFragment) {
        (field(fragment, "locationManager").get(fragment) as? LocationManager)?.removeUpdates(fragment)
        (field(fragment, "locationOverlay").get(fragment) as? MyLocationNewOverlay)?.disableMyLocation()
        field(fragment, "locationManager").set(fragment, null)
        field(fragment, "locationViewModel").set(fragment, LocationViewModel())
        field(fragment, "mapCenterState").set(fragment, null)
        val prefs = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext())
        prefs.edit().remove("latitude").remove("longitude").remove("zoom")
            .remove("pending_first_fix").putBoolean("follow", false).commit()
        MapFragment::class.java.getDeclaredMethod("initializeMapState", Bundle::class.java)
            .apply { isAccessible = true }.invoke(fragment, null)
        assertTrue(pending(fragment))
        assertEquals(51.78962, map(fragment).mapCenter.latitude, 0.0001)
    }

    @Test fun delayedFixSurvivesPauseResumeAndIsConsumedOnce() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { resetStartup(fragment(it)) }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                val f = fragment(it)
                assertTrue(pending(f))
                fix(f, Double.NaN)
                assertTrue(pending(f))
                fix(f)
                assertEquals(48.0, map(f).mapCenter.latitude, 0.0001)
                assertFalse(pending(f))
                fix(f, 49.0)
                assertEquals(48.0, map(f).mapCenter.latitude, 0.0001)
            }
        }
    }

    @Test fun interactionAndExplicitDestinationWinOverDelayedFixIncludingZero() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                val f = fragment(it)
                resetStartup(f)
                f.onUserMapInteraction()
                fix(f)
                assertEquals(51.78962, map(f).mapCenter.latitude, 0.0001)
                resetStartup(f)
                MapFragment::class.java.getDeclaredMethod("animateToLatLon", Double::class.javaPrimitiveType,
                    Double::class.javaPrimitiveType).apply { isAccessible = true }.invoke(f, 0.0, 0.0)
                map(f).controller.stopAnimation(true)
                assertFalse(pending(f))
                fix(f)
                assertEquals(0.0, map(f).mapCenter.latitude, 0.0001)
                assertEquals(0.0, map(f).mapCenter.longitude, 0.0001)
                resetStartup(f)
                fix(f, 0.0, 0.0)
                assertEquals(0.0, map(f).mapCenter.latitude, 0.0001)
            }
        }
    }

    @Test fun recreationPreservesInteractionAndSavedViewport() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                val f = fragment(it)
                resetStartup(f)
                f.onUserMapInteraction()
                map(f).setExpectedCenter(org.osmdroid.util.GeoPoint(-33.0, -70.0))
            }
            scenario.recreate()
            scenario.onActivity {
                val f = fragment(it)
                assertFalse(pending(f))
                fix(f)
                assertEquals(-33.0, map(f).mapCenter.latitude, 0.0001)
                assertEquals(-70.0, map(f).mapCenter.longitude, 0.0001)
            }
        }
    }

    @Test fun stateSavingRetainsPendingFixAndNeverReadsUninitializedMap() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                val f = fragment(it)
                resetStartup(f)
                val state = Bundle()
                f.onSaveInstanceState(state)
                assertTrue(state.getBoolean("pending_first_fix"))
                assertEquals(51.78962, state.getDouble("latitude"), 0.0001)
                // Recreate the initialization from the saved rotation state with no provider.
                field(f, "awaitingFirstFix").setBoolean(f, false)
                MapFragment::class.java.getDeclaredMethod("initializeMapState", Bundle::class.java)
                    .apply { isAccessible = true }.invoke(f, state)
                assertTrue(pending(f))
                val originalMap = map(f)
                val unlaidMap = MapView(it)
                try {
                    field(f, "mapView").set(f, unlaidMap)
                    call(f, "saveMapCenterPrefs")
                    val prefs = PreferenceManager.getDefaultSharedPreferences(it)
                    assertEquals(51.78962, prefs.getFloat("latitude", 0f).toDouble(), 0.0001)
                    field(f, "initialViewport").set(f, null)
                    prefs.edit().remove("latitude").commit()
                    call(f, "saveMapCenterPrefs")
                    assertFalse(prefs.contains("latitude"))
                } finally {
                    field(f, "mapView").set(f, originalMap)
                    unlaidMap.onDetach()
                }
            }
        }
    }
}
