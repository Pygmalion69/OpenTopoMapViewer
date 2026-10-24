package org.nitri.opentopo

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.ActionBar
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.nearby.NearbyFragment
import org.osmdroid.util.GeoPoint

@RunWith(AndroidJUnit4::class)
class NearbyHostTest {
    @Test
    fun recreationAndPermissionResultPreserveNearbyThenUpAndBackReturnToMap() {
        withNearby { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                val manager = activity.supportFragmentManager
                val nearby = manager.findFragmentById(R.id.map_container)
                assertTrue(nearby is NearbyFragment)
                assertEquals(1, manager.backStackEntryCount)
                assertTrue(activity.supportActionBar!!.displayOptions and ActionBar.DISPLAY_HOME_AS_UP != 0)

                // A late result must not replace a restored child screen.
                activity.onRequestPermissionsResult(1, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    intArrayOf(PackageManager.PERMISSION_GRANTED))
                manager.executePendingTransactions()
                assertSame(nearby, manager.findFragmentById(R.id.map_container))

                val home = PopupMenu(activity, activity.findViewById(R.id.toolbar)).menu
                    .add(0, android.R.id.home, 0, "Up")
                assertTrue(activity.onOptionsItemSelected(home))
                manager.executePendingTransactions()
                assertTrue(manager.findFragmentById(R.id.map_container) is MapFragment)
                assertEquals(0, manager.backStackEntryCount)
                activity.addNearbyFragment(GeoPoint(52.0, 5.0))
                manager.executePendingTransactions()
                activity.onBackPressedDispatcher.onBackPressed()
                manager.executePendingTransactions()
                assertTrue(manager.findFragmentById(R.id.map_container) is MapFragment)
                assertEquals(0, manager.backStackEntryCount)
            }
        }
    }

    @Test
    fun mapCallbackAfterRecreationReturnsToRestoredMap() {
        withNearby { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                // No test marker is persisted: use an empty selection to test navigation.
                activity.showNearbyPlace(null)
                activity.supportFragmentManager.executePendingTransactions()
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.map_container) is MapFragment)
                assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
            }
        }
    }

    @Test
    fun sharedContainerConsumesInsetsOnBothSidesAndResetsForFullscreen() {
        withNearby { scenario ->
            scenario.onActivity { activity ->
                val container = activity.findViewById<View>(R.id.main_container)
                val status = activity.findViewById<View>(R.id.status_bar_background)
                for (navigation in listOf(Insets.of(48, 0, 0, 0), Insets.of(0, 0, 48, 0), Insets.of(0, 0, 0, 48))) {
                    val insets = WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 24, 0, 0))
                        .setInsets(WindowInsetsCompat.Type.navigationBars(), navigation)
                        .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(16, 0, 16, 0))
                        .build()
                    assertTrue(ViewCompat.dispatchApplyWindowInsets(container, insets).isConsumed)
                    assertEquals(maxOf(navigation.left, 16), container.paddingLeft)
                    assertEquals(maxOf(navigation.right, 16), container.paddingRight)
                    assertEquals(navigation.bottom, container.paddingBottom)
                    assertEquals(24, status.layoutParams.height)
                    container.measure(
                        View.MeasureSpec.makeMeasureSpec(container.width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(container.height, View.MeasureSpec.EXACTLY)
                    )
                    container.layout(container.left, container.top, container.right, container.bottom)
                    val content = activity.findViewById<View>(R.id.map_container)
                    assertEquals(container.paddingLeft, content.left)
                    assertEquals(container.width - container.paddingRight, content.right)
                    // The ad container exists only in Play; it shares the same safe width.
                    val adId = activity.resources.getIdentifier("ad_view_container", "id", activity.packageName)
                    if (adId != 0) {
                        val ads = activity.findViewById<View>(adId)
                        assertEquals(container.paddingLeft, ads.left)
                        assertEquals(container.width - container.paddingRight, ads.right)
                        assertTrue(ads.bottom <= container.height - container.paddingBottom)
                    }
                }
                ViewCompat.dispatchApplyWindowInsets(container, WindowInsetsCompat.Builder().build())
                assertEquals(0, container.paddingLeft)
                assertEquals(0, container.paddingRight)
                assertEquals(0, container.paddingBottom)
                assertEquals(View.GONE, status.visibility)
                ViewCompat.requestApplyInsets(container)
            }
        }
    }

    private fun withNearby(block: (ActivityScenario<MainActivity>) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Grant location permission before running host integration tests",
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
                activity.addNearbyFragment(GeoPoint(52.0, 5.0))
                activity.supportFragmentManager.executePendingTransactions()
            }
            block(scenario)
        }
    }
}
