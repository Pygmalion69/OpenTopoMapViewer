package org.nitri.opentopo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.Manifest
import androidx.test.espresso.NoMatchingViewException
import org.hamcrest.Matchers.anyOf

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Test
    fun testMapIsDisplayed() {
        onView(withId(R.id.mapview)).check(matches(isDisplayed()))
    }

    @Test
    fun testToggleFollowMode() {
        val followMatcher = allOf(withId(R.id.action_follow), isDisplayed())
        val noFollowMatcher = allOf(withId(R.id.action_no_follow), isDisplayed())

        try {
            // Check if follow is active
            onView(followMatcher).check(matches(isDisplayed()))
            onView(followMatcher).perform(click())
            onView(noFollowMatcher).check(matches(isDisplayed()))
        } catch (e: NoMatchingViewException) {
            // Otherwise try no-follow
            onView(noFollowMatcher).perform(click())
            onView(followMatcher).check(matches(isDisplayed()))
        }
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
        dismissConsentIfPresent()
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(allOf(withText(R.string.settings), isDisplayed())).perform(click())
        onView(withId(R.id.settings)).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenNearby() {
        dismissConsentIfPresent()
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(allOf(withText(R.string.nearby), isDisplayed())).perform(click())
        onView(withId(R.id.nearby_recycler_view)).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenMarkers() {
        dismissConsentIfPresent()
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(allOf(withText(R.string.markers), isDisplayed())).perform(click())
        onView(withId(R.id.markerRecyclerView)).check(matches(isDisplayed()))
    }

    private fun dismissConsentIfPresent() {
        try {
            // Attempt to click on a common consent button if we are stuck on a consent screen
            // Since it's often a webview or internal view, we try common button texts
            onView(anyOf(withText("Accept"), withText("Agree"), withText("Consent"), withText("OK")))
                .perform(click())
        } catch (e: NoMatchingViewException) {
            // No consent form found, proceed
        }
    }
}
