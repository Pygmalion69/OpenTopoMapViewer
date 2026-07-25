package org.nitri.opentopo.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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

    @Before
    fun setup() {
        renderer = MarkerAnnotationRenderer(context)
    }

    @Test
    fun hitTest_returnsFalseWhenNotDrawn() {
        assertFalse(renderer.hitTest(100f, 100f))
    }

    @Test
    fun hitTest_returnsFalseAfterClear() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        `when`(projection.orientation).thenReturn(0f)
        
        renderer.draw(canvas, projection, 100f, 100f, "Test", 40, 1.0f, 1000, 1000)
        renderer.clear()
        assertFalse(renderer.hitTest(100f, 50f))
    }

    @Test
    fun hitTest_basicCentering() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        `when`(projection.orientation).thenReturn(0f)
        
        // Pivot at (500, 500)
        renderer.draw(canvas, projection, 500f, 500f, "Label", 50, 1.0f, 1000, 1000)
        
        // The label is drawn ABOVE (500, 500). 
        // Anchor bottom is at y=500. gap is 2dp. 
        // So label bottom is at 500 - 50*1.0 - 2*density.
        // Let's just verify it's horizontal and clickable at the pivot X.
        assertTrue(renderer.hitTest(500f, 400f)) 
        assertFalse(renderer.hitTest(100f, 100f))
    }

    @Test
    fun clamping_leftEdge() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        `when`(projection.orientation).thenReturn(0f)
        
        // Marker at (5, 500) - label would be off-screen to the left if not clamped
        renderer.draw(canvas, projection, 5f, 500f, "Long Long Label", 50, 1.0f, 1000, 1000)
        
        // Hit test at x=10 should pass if it was clamped to 0
        assertTrue(renderer.hitTest(10f, 400f))
    }

    @Test
    fun clamping_topEdge() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        `when`(projection.orientation).thenReturn(0f)
        
        // Marker at (500, 5) - label would be off-screen to the top if not clamped
        renderer.draw(canvas, projection, 500f, 5f, "Label", 50, 1.0f, 1000, 1000)
        
        // Hit test at y=10 should pass if it was clamped to 0
        assertTrue(renderer.hitTest(500f, 10f))
    }

    @Test
    fun rotation_hitTest() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        `when`(projection.orientation).thenReturn(90f) // 90 degrees rotation
        
        renderer.draw(canvas, projection, 500f, 500f, "Label", 50, 1.0f, 1000, 1000)
        
        // The label is drawn horizontal to the SCREEN. 
        // Our counter-rotation makes it horizontal on screen regardless of map orientation.
        // So at (500, 400) screen space, it should be clickable.
        assertTrue(renderer.hitTest(500f, 400f))
    }
}
