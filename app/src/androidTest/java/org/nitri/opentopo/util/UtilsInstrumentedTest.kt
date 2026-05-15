package org.nitri.opentopo.util

import androidx.appcompat.content.res.AppCompatResources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.WayPoint
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
        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_action_layers)
        requireNotNull(drawable)

        val bitmap = Utils.getBitmapFromDrawable(context, R.drawable.ic_action_layers, 255)

        assertNotNull(bitmap)
        assertEquals(drawable.intrinsicWidth, bitmap.width)
        assertEquals(drawable.intrinsicHeight, bitmap.height)
    }

    @Test
    fun elevationFromNmea_validSentenceReturnsElevation() {
        val nmea = "\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,100.0,M,46.9,M,,*47"
        val elevation = Utils.elevationFromNmea(nmea)
        assertEquals(100.0, elevation, 0.0)
    }

    @Test
    fun elevationFromNmea_invalidSentenceReturnsNoElevationValue() {
        val elevation = Utils.elevationFromNmea("INVALID")
        assertEquals(Utils.NO_ELEVATION_VALUE.toDouble(), elevation, 0.0)
    }

    @Test
    fun getWayPointTypes_returnsSortedDistinctTypes() {
        val points = listOf(
            WayPoint.Builder().setType("summit").build() as WayPoint,
            WayPoint.Builder().setType("cafe").build() as WayPoint,
            WayPoint.Builder().setType("").build() as WayPoint,
            WayPoint.Builder().setType("summit").build() as WayPoint
        )
        val gpx = Gpx.Builder().setTracks(emptyList()).setRoutes(emptyList()).setWayPoints(points).build()
        val types = Utils.getWayPointTypes(gpx, "default")
        assertEquals(listOf("cafe", "default", "summit"), types)
    }

    @Test
    fun getWayPointsByType_filtersCorrectly() {
        val summit = WayPoint.Builder().setType("summit").build() as WayPoint
        val blank = WayPoint.Builder().setType("").build() as WayPoint
        val gpx = Gpx.Builder().setTracks(emptyList()).setRoutes(emptyList()).setWayPoints(listOf(summit, blank)).build()
        assertEquals(1, Utils.getWayPointsByType(gpx, "summit").size)
        assertEquals(1, Utils.getWayPointsByType(gpx, "").size)
    }
}
