package org.nitri.opentopo.analytics

import io.ticofab.androidgpxparser.parser.domain.Gpx

/**
 * AnalyticsTracker abstracts analytics reporting so that FOSS stays clean
 * and Play flavor can provide a Firebase-backed implementation.
 */
interface AnalyticsTracker {
    fun trackScreen(screenName: String, screenClass: String)

    fun trackGpxLoaded(
        source: String, // e.g., "file" or other sources
        gpx: Gpx,
        fileName: String?
    )

    fun trackKmlLoaded(
        source: String,
        contentType: String,
        fileName: String?
    )

    fun trackRouteCalculated(
        profile: String?,
        waypointCount: Int
    )

    fun trackMapLayerSelected(baseMap: String, overlay: String)

    fun trackMarkersImported(importedCount: Int, skippedCount: Int)

    fun trackMarkersExported(markerCount: Int)

    fun trackMarkersDeleted(markerCount: Int)
}

/** No-op implementation used by non-Play builds. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun trackScreen(screenName: String, screenClass: String) { /* no-op */ }
    override fun trackGpxLoaded(source: String, gpx: Gpx, fileName: String?) { /* no-op */ }
    override fun trackKmlLoaded(source: String, contentType: String, fileName: String?) { /* no-op */ }
    override fun trackRouteCalculated(profile: String?, waypointCount: Int) { /* no-op */ }
    override fun trackMapLayerSelected(baseMap: String, overlay: String) { /* no-op */ }
    override fun trackMarkersImported(importedCount: Int, skippedCount: Int) { /* no-op */ }
    override fun trackMarkersExported(markerCount: Int) { /* no-op */ }
    override fun trackMarkersDeleted(markerCount: Int) { /* no-op */ }
}
