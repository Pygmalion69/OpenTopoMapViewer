package org.nitri.opentopo.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.ticofab.androidgpxparser.parser.domain.Gpx

private const val EVENT_GPX_LOADED = "gpx_loaded"

class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {

    private val firebase = FirebaseAnalytics.getInstance(context)

    override fun trackGpxLoaded(source: String, gpx: Gpx, fileName: String?) {
        val params = Bundle().apply {
            putString("source", source)
            fileName?.let { putString("file_name", it) }
            putInt("track_count", gpx.tracks?.size ?: 0)
            putInt("route_count", gpx.routes?.size ?: 0)
            putInt("waypoint_count", gpx.wayPoints?.size ?: 0)
        }
        firebase.logEvent(EVENT_GPX_LOADED, params)
    }
}
