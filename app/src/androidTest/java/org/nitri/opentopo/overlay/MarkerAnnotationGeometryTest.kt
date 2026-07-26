package org.nitri.opentopo.overlay

import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.osmdroid.views.Projection

@RunWith(AndroidJUnit4::class)
class MarkerAnnotationGeometryTest {

    private lateinit var renderer: MarkerAnnotationRenderer
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val EPSILON = 0.1f

    @Before
    fun setup() {
        renderer = MarkerAnnotationRenderer(context)
    }

    @Test
    fun hitTest_returnsFalseWhenNotDrawn() {
        assertFalse(renderer.hitTest(100f, 100f))
    }

    @Test
    fun clear_resetsDrawnStateAndGeometry() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        `when`(projection.orientation).thenReturn(0f)
        
        renderer.draw(canvas, projection, 500f, 500f, "Test", Color.RED, 50, 1.0f, 1000, 1000)
        assertNotNull(renderer.getLastGeometry())
        
        renderer.clear()
        assertNull(renderer.getLastGeometry())
        assertFalse(renderer.hitTest(500f, 400f))
    }

    @Test
    fun geometry_rotationCorrectness() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        
        val orientations = listOf(0f, 45f, 90f, 180f, 270f)
        
        for (orientation in orientations) {
            `when`(projection.orientation).thenReturn(orientation)
            renderer.draw(canvas, projection, 500f, 500f, "Label", Color.RED, 50, 1.0f, 1000, 1000)
            
            val geometry = renderer.getLastGeometry()!!
            
            // Local center
            val localPoint = floatArrayOf(
                geometry.localBounds.centerX(),
                geometry.localBounds.centerY()
            )
            
            // Mathematically transform to screen space using the same matrix exposed by renderer
            val transformedPoint = FloatArray(2)
            geometry.localToScreen.mapPoints(transformedPoint, localPoint)
            
            // Must hit the transformed center
            assertTrue("Should hit at orientation $orientation", renderer.hitTest(transformedPoint[0], transformedPoint[1]))
            
            // Point well outside
            assertFalse("Should not hit outside at orientation $orientation", renderer.hitTest(transformedPoint[0] + 500f, transformedPoint[1]))
        }
    }

    @Test
    fun clamping_keepsAllCornersInsideViewport() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        val viewWidth = 1000
        val viewHeight = 1000
        
        val orientations = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
        val corners = listOf(
            Pair(0f, 0f),      // Top-left
            Pair(1000f, 0f),    // Top-right
            Pair(0f, 1000f),    // Bottom-left
            Pair(1000f, 1000f)  // Bottom-right
        )
        
        for (orientation in orientations) {
            for ((x, y) in corners) {
                `when`(projection.orientation).thenReturn(orientation)
                renderer.draw(canvas, projection, x, y, "Long Label Content", Color.RED, 50, 1.0f, viewWidth, viewHeight)
                
                val geometry = renderer.getLastGeometry()!!
                
                // Assert that the screen-space bounding box is within the viewport
                assertTrue("Left clamped at orientation $orientation at ($x, $y). Bounds: ${geometry.screenBounds}", geometry.screenBounds.left >= -EPSILON)
                assertTrue("Top clamped at orientation $orientation at ($x, $y). Bounds: ${geometry.screenBounds}", geometry.screenBounds.top >= -EPSILON)
                assertTrue("Right clamped at orientation $orientation at ($x, $y). Bounds: ${geometry.screenBounds}", geometry.screenBounds.right <= viewWidth + EPSILON)
                assertTrue("Bottom clamped at orientation $orientation at ($x, $y). Bounds: ${geometry.screenBounds}", geometry.screenBounds.bottom <= viewHeight + EPSILON)
            }
        }
    }

    @Test
    fun textWidth_isConstrainedAndEllipsized() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        `when`(projection.orientation).thenReturn(0f)
        
        val longText = "This is a very long marker name that should definitely be ellipsized to stay within the 120dp limit."
        renderer.draw(canvas, projection, 500f, 500f, longText, Color.RED, 50, 1.0f, 1000, 1000)
        
        val geometry = renderer.getLastGeometry()!!
        
        val density = context.resources.displayMetrics.density
        val maxAllowedContentWidth = 120f * density
        val horizontalPadding = 4f * density * 2
        
        // geometry.localBounds.width() should be contentWidth + padding
        assertTrue("Label width ${geometry.localBounds.width()} should be constrained near ${maxAllowedContentWidth + horizontalPadding}", 
            geometry.localBounds.width() <= maxAllowedContentWidth + horizontalPadding + EPSILON)
    }

    @Test
    fun haloColor_adaptiveSelection() {
        // Red is dark (luminance ~0.21) -> should have light halo
        val darkColor = Color.RED
        val haloForDark = renderer.haloColorFor(darkColor)
        assertEquals(255, Color.red(haloForDark)) // White/near-white
        
        // Yellow is light (luminance ~0.92) -> should have dark halo
        val lightColor = Color.YELLOW
        val haloForLight = renderer.haloColorFor(lightColor)
        assertEquals(32, Color.red(haloForLight)) // Dark charcoal
    }
}
