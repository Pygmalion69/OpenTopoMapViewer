package org.nitri.opentopo.analytics

import io.ticofab.androidgpxparser.parser.domain.Gpx

/**
 * AnalyticsTracker abstracts analytics reporting so that FOSS stays clean
 * and Play flavor can provide a Firebase-backed implementation.
 */
interface AnalyticsTracker {
    fun trackGpxLoaded(
        source: String, // e.g., "file" or other sources
        gpx: Gpx,
        fileName: String?
    )

    fun trackRouteCalculated(
        profile: String?,
        waypointCount: Int
    )
}

/** No-op implementation used by non-Play builds. */
private object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun trackGpxLoaded(source: String, gpx: Gpx, fileName: String?) { /* no-op */ }
    override fun trackRouteCalculated(profile: String?, waypointCount: Int) { /* no-op */ }
}

