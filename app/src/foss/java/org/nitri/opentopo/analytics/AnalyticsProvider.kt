package org.nitri.opentopo.analytics

import android.content.Context

/**
 * FOSS flavor provider that returns a no-op tracker.
 */
object AnalyticsProvider {
    @Synchronized
    fun get(context: Context? = null): AnalyticsTracker = NoOpAnalyticsTracker
}
