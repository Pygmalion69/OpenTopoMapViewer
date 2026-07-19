package org.nitri.opentopo.util

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.defaultMarkerColor
import org.nitri.opentopo.SettingsActivity
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
}
