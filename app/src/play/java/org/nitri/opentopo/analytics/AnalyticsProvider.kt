package org.nitri.opentopo.analytics

import android.content.Context

/**
 * Play flavor provider that returns a Firebase-backed tracker.
 */
object AnalyticsProvider {
    private var instance: AnalyticsTracker? = null

    @Synchronized
    fun get(context: Context? = null): AnalyticsTracker {
        val ctx = requireNotNull(context) { "Context must not be null for Play analytics" }
        if (instance == null) {
            instance = FirebaseAnalyticsTracker(ctx.applicationContext)
        }
        return instance as AnalyticsTracker
    }
}
