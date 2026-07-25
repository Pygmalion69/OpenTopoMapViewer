package org.nitri.opentopo.util

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.defaultGpxTrackColor
import org.nitri.opentopo.SettingsActivity
import org.nitri.opentopo.ui.color.DEFAULT_GPX_TRACK_COLOR

@RunWith(AndroidJUnit4::class)
class GpxPreferenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun defaultGpxTrackColor_returnsDefaultWhenUnset() {
        assertEquals(DEFAULT_GPX_TRACK_COLOR, context.defaultGpxTrackColor())
    }

    @Test
    fun defaultGpxTrackColor_returnsStoredValue() {
        val customColor = 0xFF654321.toInt()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(SettingsActivity.PREF_GPX_TRACK_COLOR, customColor)
            .commit()
        
        assertEquals(customColor, context.defaultGpxTrackColor())
    }
}
