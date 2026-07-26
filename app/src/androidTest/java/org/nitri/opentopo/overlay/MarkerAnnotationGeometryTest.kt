package org.nitri.opentopo.overlay

import android.graphics.Canvas
import android.graphics.Matrix
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
    fun hitTest_rotationCorrectness() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        
        // Pivot at (500, 500), 90 degrees rotation
        `when`(projection.orientation).thenReturn(90f)
        renderer.draw(canvas, projection, 500f, 500f, "Label", 50, 1.0f, 1000, 1000)
        
        // At 90 degrees map orientation, our label is counter-rotated -90 around (500, 500).
        // It should still be horizontal on screen.
        // Let's use the actual matrix to find a point that SHOULD hit.
        
        val matrix = Matrix()
        matrix.setRotate(-90f, 500f, 500f)
        
        // The unrotated background rect is above the anchor.
        // Let's find it via reflection or just use the same logic.
        val density = context.resources.displayMetrics.density
        val gapPx = 2f * density
        // Approximate height based on 12sp text
        val labelHeight = 15f * density 
        
        // A point that would be inside the unrotated label (centered above anchor)
        val localPoint = floatArrayOf(500f, 500f - 50f - gapPx - labelHeight / 2f)
        val transformedPoint = FloatArray(2)
        matrix.mapPoints(transformedPoint, localPoint)
        
        // Tapping the mathematically transformed point should hit
        assertTrue(renderer.hitTest(transformedPoint[0], transformedPoint[1]))
    }

    @Test
    fun clamping_keepsLabelInViewport() {
        val canvas = mock(Canvas::class.java)
        val projection = mock(Projection::class.java)
        `when`(projection.orientation).thenReturn(45f)
        
        // Marker at (0, 0) - rotated label would be mostly off-screen
        renderer.draw(canvas, projection, 0f, 0f, "Label", 50, 1.0f, 1000, 1000)
        
        // After clamping, hit testing a point at (10, 10) should likely hit
        // (depending on label size, but it should be shifted into view)
        // More robust: verify it hits SOME point that was previously off-screen
        assertTrue(renderer.hitTest(15f, 15f))
    }
}
