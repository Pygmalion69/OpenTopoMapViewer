package org.nitri.opentopo.ui.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

const val DEFAULT_MARKER_COLOR: Int = 0xCCCC3F34.toInt()
const val DEFAULT_GPX_TRACK_COLOR: Int = 0xFFF44336.toInt()

val APP_COLOR_PALETTE: List<Int> = listOf(
    0xFFF44336.toInt(), // Red
    0xFFE91E63.toInt(), // Pink
    0xFF9C27B0.toInt(), // Purple
    0xFF673AB7.toInt(), // Deep Purple
    0xFF3F51B5.toInt(), // Indigo
    0xFF2196F3.toInt(), // Blue
    0xFF03A9F4.toInt(), // Light Blue
    0xFF00BCD4.toInt(), // Cyan
    0xFF009688.toInt(), // Teal
    0xFF4CAF50.toInt(), // Green
    0xFF8BC34A.toInt(), // Light Green
    0xFFCDDC39.toInt(), // Lime
    0xFFFFEB3B.toInt(), // Yellow
    0xFFFFC107.toInt(), // Amber
    0xFFFF9800.toInt(), // Orange
    0xFFFF5722.toInt(), // Deep Orange
    0xFF795548.toInt(), // Brown
    0xFF9E9E9E.toInt(), // Grey
    0xFF607D8B.toInt(), // Blue Grey
    DEFAULT_MARKER_COLOR // Legacy Red
)

val OPAQUE_APP_COLOR_PALETTE: List<Int> = APP_COLOR_PALETTE.filter { (it ushr 24) == 0xFF }

/**
 * Returns a contrast color (Black or White) for a given background color.
 */
fun getContrastColor(backgroundColor: Int): Color {
    return if (Color(backgroundColor).luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}
