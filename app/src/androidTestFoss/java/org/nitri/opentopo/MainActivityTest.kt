package org.nitri.opentopo

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(permissionRule).around(activityRule)

    @Before
    fun setUp() {
        Intents.init()
        val device = UiDevice.getInstance(getInstrumentation())
        
        // Dismiss permission dialog if it appears despite GrantPermissionRule
        val allowButton = device.findObject(UiSelector().textMatches("(?i)While using the app|Allow|Allow all the time"))
        if (allowButton.exists()) {
            allowButton.click()
        }

        // Wait for map to be displayed to ensure activity is ready
        onView(withId(R.id.mapview)).check(matches(isDisplayed()))
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testMapIsDisplayed() {
        onView(withId(R.id.mapview)).check(matches(isDisplayed()))
    }

    @Test
    fun testMapLongPress() {
        onView(withId(R.id.mapview)).perform(longClick())
    }
    
    @Test
    fun testFullscreenToggle() {
        onView(withId(R.id.mapview)).perform(click())
    }

    @Test
    fun testOpenSettings() {
        clickMenuItem(R.id.action_settings)
        onView(withId(R.id.settings)).check(matches(isDisplayed()))
        pressBack()
    }

    @Test
    fun testOpenNearby() {
        clickMenuItem(R.id.action_nearby)
        onView(withId(R.id.nearby_recycler_view)).check(matches(isDisplayed()))
        pressBack()
    }

    @Test
    fun testOpenMarkers() {
        clickMenuItem(R.id.action_markers)
        onView(withId(R.id.markerRecyclerView)).check(matches(isDisplayed()))
        pressBack()
    }

    @Test
    fun testToggleFollowMode() {
        val followIds = listOf(R.id.action_follow, R.id.action_no_follow)
        var found = false
        for (id in followIds) {
            try {
                onView(withId(id)).check(matches(isDisplayed())).perform(click())
                found = true
                break
            } catch (_: Throwable) {}
        }
        org.junit.Assert.assertTrue("Neither follow nor no-follow action found", found)
    }

    private fun clickMenuItem(menuItemId: Int) {
        try {
            onView(withId(menuItemId)).check(matches(isDisplayed())).perform(click())
        } catch (e: Exception) {
            openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
            onView(withId(menuItemId)).perform(click())
        }
    }
}
