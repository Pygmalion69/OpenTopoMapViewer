package org.nitri.opentopo.ui.color

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorContrastTest {

    @Test
    fun getContrastColor_returnsWhiteForDarkBackground() {
        val black = 0xFF000000.toInt()
        assertEquals(Color.White, getContrastColor(black))
    }

    @Test
    fun getContrastColor_returnsBlackForLightBackground() {
        val white = 0xFFFFFFFF.toInt()
        assertEquals(Color.Black, getContrastColor(white))
    }

    @Test
    fun getContrastColor_returnsWhiteForDeepPurple() {
        val deepPurple = 0xFF673AB7.toInt()
        assertEquals(Color.White, getContrastColor(deepPurple))
    }

    @Test
    fun getContrastColor_returnsBlackForYellow() {
        val yellow = 0xFFFFEB3B.toInt()
        assertEquals(Color.Black, getContrastColor(yellow))
    }
}
