package org.nitri.opentopo.analytics

import android.content.Context
import io.ticofab.androidgpxparser.parser.domain.Gpx

/**
 * FOSS flavor provider that returns a no-op tracker.
 */
object AnalyticsProvider {
    private var instance: AnalyticsTracker? = null

    @Synchronized
    fun get(context: Context? = null): AnalyticsTracker {
        if (instance == null) {
            instance = object : AnalyticsTracker {
                override fun trackGpxLoaded(source: String, gpx: Gpx, fileName: String?) {
                    // no-op in FOSS flavor
                }
                override fun trackRouteCalculated(profile: String?, waypointCount: Int) {
                    // no-op in FOSS flavor
                }
            }
        }
        return instance as AnalyticsTracker
    }
}
