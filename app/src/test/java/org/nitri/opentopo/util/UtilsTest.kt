package org.nitri.opentopo.util

import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.Route
import io.ticofab.androidgpxparser.parser.domain.RoutePoint
import io.ticofab.androidgpxparser.parser.domain.Track
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import io.ticofab.androidgpxparser.parser.domain.TrackSegment
import io.ticofab.androidgpxparser.parser.domain.WayPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilsTest {

    private fun buildGpx(
        tracks: List<Track> = emptyList(),
        routes: List<Route> = emptyList(),
        wayPoints: List<WayPoint> = emptyList()
    ): Gpx {
        return Gpx.Builder()
            .setTracks(tracks)
            .setRoutes(routes)
            .setWayPoints(wayPoints)
            .build()
    }

    @Test
    fun area_usesTrackPointsWhenPresent() {
        val trackPoints = listOf(
            TrackPoint.Builder().setLatitude(10.0).setLongitude(20.0).build() as TrackPoint,
            TrackPoint.Builder().setLatitude(12.0).setLongitude(18.0).build() as TrackPoint,
            TrackPoint.Builder().setLatitude(8.0).setLongitude(25.0).build() as TrackPoint
        )
        val track = Track.Builder()
            .setTrackSegments(listOf(TrackSegment.Builder().setTrackPoints(trackPoints).build()))
            .build()

        val bounds = Utils.area(buildGpx(tracks = listOf(track)))

        assertEquals(12.0, bounds.latNorth, 0.0)
        assertEquals(8.0, bounds.latSouth, 0.0)
        assertEquals(25.0, bounds.lonEast, 0.0)
        assertEquals(18.0, bounds.lonWest, 0.0)
    }

    @Test
    fun getWayPointTypes_returnsSortedDistinctTypes() {
        val points = listOf(
            WayPoint.Builder().setType("summit").build() as WayPoint,
            WayPoint.Builder().setType("cafe").build() as WayPoint,
            WayPoint.Builder().setType("").build() as WayPoint,
            WayPoint.Builder().setType("summit").build() as WayPoint
        )
        val types = Utils.getWayPointTypes(buildGpx(wayPoints = points), "default")
        assertEquals(listOf("cafe", "default", "summit"), types)
    }

    @Test
    fun getWayPointsByType_filtersCorrectly() {
        val summit = WayPoint.Builder().setType("summit").build() as WayPoint
        val blank = WayPoint.Builder().setType("").build() as WayPoint
        val gpx = buildGpx(wayPoints = listOf(summit, blank))

        assertEquals(1, Utils.getWayPointsByType(gpx, "summit").size)
        assertEquals(1, Utils.getWayPointsByType(gpx, "").size)
    }

    @Test
    fun convertRouteToTrack_createsSyntheticTrackWhenNoTrackExists() {
        val routePoints = listOf(
            RoutePoint.Builder().setLatitude(1.0).setLongitude(2.0).setDesc("a").build() as RoutePoint,
            RoutePoint.Builder().setLatitude(3.0).setLongitude(4.0).setDesc("b").build() as RoutePoint
        )
        val route = Route.Builder().setRoutePoints(routePoints).build()
        val gpx = buildGpx(routes = listOf(route))

        val converted = Utils.convertRouteToTrack(gpx)

        assertEquals(1, converted.tracks?.size)
        assertEquals(2, converted.tracks?.first()?.trackSegments?.first()?.trackPoints?.size)
        assertTrue(converted.routes?.isNotEmpty() == true)
    }

    @Test
    fun convertRouteToTrack_returnsOriginalWhenTrackPresent() {
        val existingTrack = Track.Builder().setTrackSegments(emptyList()).build()
        val gpx = buildGpx(tracks = listOf(existingTrack))

        val converted = Utils.convertRouteToTrack(gpx)

        assertSame(gpx, converted)
    }
}
