package org.nitri.opentopo.util

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.R

@RunWith(AndroidJUnit4::class)
class UtilsInstrumentedTest {

    @Test
    fun getBitmapFromDrawable_returnsBitmapWithIntrinsicDimensions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val drawable = androidx.appcompat.content.res.AppCompatResources.getDrawable(context, R.drawable.ic_action_layers)
        requireNotNull(drawable)

        val bitmap = Utils.getBitmapFromDrawable(context, R.drawable.ic_action_layers)

        assertNotNull(bitmap)
        assertEquals(drawable.intrinsicWidth, bitmap.width)
        assertEquals(drawable.intrinsicHeight, bitmap.height)
    }
}
