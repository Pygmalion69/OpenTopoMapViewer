package org.nitri.opentopo.analytics

/**
 * Shared analytics vocabulary. Main source set code only depends on this
 * interface layer; flavor source sets decide whether anything is sent.
 */
object AnalyticsNames {
    object Screen {
        const val MAP = "map"
        const val GPX_DETAIL = "gpx_detail"
        const val NEARBY = "nearby"
        const val MARKERS = "markers"
        const val SETTINGS = "settings"
        const val CACHE_SETTINGS = "cache_settings"
    }

    object ContentType {
        const val GPX = "gpx"
        const val KML = "kml"
        const val KMZ = "kmz"
        const val MARKERS = "markers"
    }
}
