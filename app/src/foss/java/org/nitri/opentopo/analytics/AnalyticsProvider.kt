package org.nitri.opentopo.analytics

import android.content.Context

/**
 * FOSS flavor provider that returns a no-op tracker.
 */
object AnalyticsProvider {
    private var instance: AnalyticsTracker? = null

    @Synchronized
    fun get(context: Context? = null): AnalyticsTracker {
        if (instance == null) {
            instance = object : AnalyticsTracker {
                override fun trackGpxLoaded(source: String, gpx: io.ticofab.androidgpxparser.parser.domain.Gpx, fileName: String?) {
                    // no-op in FOSS flavor
                }
            }
        }
        return instance as AnalyticsTracker
    }
}
