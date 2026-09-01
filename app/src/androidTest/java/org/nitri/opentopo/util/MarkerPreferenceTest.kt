package org.nitri.opentopo.util

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.defaultMarkerColor
import org.nitri.opentopo.markerLabelMinimumZoom
import org.nitri.opentopo.SettingsActivity
import org.nitri.opentopo.overlay.DEFAULT_MARKER_LABEL_MIN_ZOOM
import org.nitri.opentopo.ui.color.DEFAULT_MARKER_COLOR

@RunWith(AndroidJUnit4::class)
class MarkerPreferenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun defaultMarkerColor_returnsLegacyDefaultWhenUnset() {
        assertEquals(DEFAULT_MARKER_COLOR, context.defaultMarkerColor())
    }

    @Test
    fun defaultMarkerColor_returnsStoredValue() {
        val customColor = 0xFF123456.toInt()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(SettingsActivity.PREF_DEFAULT_MARKER_COLOR, customColor)
            .commit()
        
        assertEquals(customColor, context.defaultMarkerColor())
    }

    @Test
    fun showMarkerLabels_defaultsToFalse() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        assertEquals(false, prefs.getBoolean(SettingsActivity.PREF_SHOW_MARKER_LABELS, false))
    }

    @Test
    fun markerLabelMinimumZoom_defaultsTo14() {
        assertEquals(DEFAULT_MARKER_LABEL_MIN_ZOOM, context.markerLabelMinimumZoom(), 0.0)
    }

    @Test
    fun markerLabelMinimumZoom_returnsStoredValue() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(SettingsActivity.PREF_MARKER_LABEL_MIN_ZOOM, "11")
            .commit()
        assertEquals(11.0, context.markerLabelMinimumZoom(), 0.0)
    }

    @Test
    fun markerLabelMinimumZoom_handlesInvalidValue() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(SettingsActivity.PREF_MARKER_LABEL_MIN_ZOOM, "invalid")
            .commit()
        assertEquals(DEFAULT_MARKER_LABEL_MIN_ZOOM, context.markerLabelMinimumZoom(), 0.0)
    }
}
