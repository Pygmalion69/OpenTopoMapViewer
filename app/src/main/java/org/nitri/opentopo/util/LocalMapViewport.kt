package org.nitri.opentopo.util

import kotlin.math.*

internal data class LocalMapTarget(val zoom: Int, val fallback: XyzTile?)
internal data class LocalMapBounds(val west: Double, val south: Double, val east: Double, val north: Double)

/** Fit at an actual package zoom; a sparse center must never strand the user in a hole. */
internal object LocalMapViewport {
    fun choose(
        tiles: Set<XyzTile>, fittedZoom: Double, latitude: Double, longitude: Double,
        visibleBounds: LocalMapBounds? = null
    ): LocalMapTarget {
        require(tiles.isNotEmpty())
        val zooms = tiles.map { it.z }.distinct().sorted()
        val zoom = zooms.lastOrNull { it <= fittedZoom } ?: zooms.first()
        val n = 2.0.pow(zoom)
        val lat = Math.toRadians(latitude.coerceIn(-85.05112878, 85.05112878))
        val center = XyzTile(zoom, floor((longitude + 180) / 360 * n).toInt(),
            floor((1 - ln(tan(lat) + 1 / cos(lat)) / PI) / 2 * n).toInt())
        val visible = if (visibleBounds == null) center in tiles else {
            fun x(lon: Double) = floor((lon + 180) / 360 * n).toInt().coerceIn(0, n.toInt() - 1)
            fun y(latitude: Double): Int {
                val radians = Math.toRadians(latitude.coerceIn(-85.05112878, 85.05112878))
                return floor((1 - ln(tan(radians) + 1 / cos(radians)) / PI) / 2 * n)
                    .toInt().coerceIn(0, n.toInt() - 1)
            }
            val west = x(visibleBounds.west)
            val east = x(visibleBounds.east)
            val north = y(visibleBounds.north)
            val south = y(visibleBounds.south)
            tiles.any { tile ->
                tile.z == zoom && tile.y in north..south &&
                    (if (visibleBounds.west <= visibleBounds.east) tile.x in west..east
                    else tile.x >= west || tile.x <= east)
            }
        }
        return LocalMapTarget(zoom, if (visible) null else
            tiles.filter { it.z == zoom }.minWith(compareBy<XyzTile> { it.x }.thenBy { it.y }))
    }
}
