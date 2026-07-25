package org.nitri.opentopo.ui.color

import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {

    @Test
    fun testPalette() {
        assertTrue("Default GPX color should be in the palette",
            APP_COLOR_PALETTE.contains(DEFAULT_GPX_TRACK_COLOR))
        
        assertTrue("Legacy marker color should be in the palette",
            APP_COLOR_PALETTE.contains(DEFAULT_MARKER_COLOR))
    }
}
