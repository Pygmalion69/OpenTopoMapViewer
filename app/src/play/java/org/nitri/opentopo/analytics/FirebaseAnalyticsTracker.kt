package org.nitri.opentopo.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.ticofab.androidgpxparser.parser.domain.Gpx

private const val EVENT_GPX_LOADED = "gpx_loaded"
private const val EVENT_KML_LOADED = "kml_loaded"
private const val EVENT_ORS_ROUTE_RESULT = "ors_route_result"
private const val EVENT_ORS_SEARCH_OPENED = "ors_search_opened"
private const val EVENT_ORS_SEARCH_RESULT = "ors_search_result"
private const val EVENT_ORS_SEARCH_SELECTION = "ors_search_selection"
private const val EVENT_MAP_LAYER_SELECTED = "map_layer_selected"
private const val EVENT_MARKERS_IMPORTED = "markers_imported"
private const val EVENT_MARKERS_EXPORTED = "markers_exported"
private const val EVENT_MARKERS_DELETED = "markers_deleted"

class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {

    private val firebase = FirebaseAnalytics.getInstance(context)

    override fun trackScreen(screenName: String, screenClass: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }

    override fun trackGpxLoaded(source: String, gpx: Gpx, fileName: String?) {
        val params = Bundle().apply {
            putString("source", source)
            putInt("track_count", gpx.tracks?.size ?: 0)
            putInt("route_count", gpx.routes?.size ?: 0)
            putInt("waypoint_count", gpx.wayPoints?.size ?: 0)
        }
        firebase.logEvent(EVENT_GPX_LOADED, params)
    }

    override fun trackKmlLoaded(source: String, contentType: String, fileName: String?) {
        val params = Bundle().apply {
            putString("source", source)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
        }
        firebase.logEvent(EVENT_KML_LOADED, params)
    }

    override fun trackOrsRouteResult(outcome: OrsOutcome, profile: String, destinationCount: Int,
        startSource: OrsStartSource, durationBucket: DurationBucket, errorCategory: OrsErrorCategory?) {
        val params = Bundle().apply {
            putString("outcome", outcome.value)
            putString("profile", profile)
            putInt("destination_count", destinationCount)
            putString("start_source", startSource.value)
            putString("duration_bucket", durationBucket.value)
            if (outcome == OrsOutcome.ERROR) errorCategory?.let { putString("error_category", it.value) }
        }
        firebase.logEvent(EVENT_ORS_ROUTE_RESULT, params)
    }

    override fun trackOrsSearchOpened(hasMapFocus: Boolean) {
        firebase.logEvent(EVENT_ORS_SEARCH_OPENED, Bundle().apply { putBoolean("has_map_focus", hasMapFocus) })
    }

    override fun trackOrsSearchResult(outcome: OrsOutcome, resultCount: Int,
        durationBucket: DurationBucket, queryLengthBucket: QueryLengthBucket,
        isRetry: Boolean, errorCategory: OrsErrorCategory?) {
        firebase.logEvent(EVENT_ORS_SEARCH_RESULT, Bundle().apply {
            putString("outcome", outcome.value)
            putInt("result_count", resultCount.coerceIn(0, 10))
            putString("duration_bucket", durationBucket.value)
            putString("query_length_bucket", queryLengthBucket.value)
            putBoolean("is_retry", isRetry)
            if (outcome == OrsOutcome.ERROR) errorCategory?.let { putString("error_category", it.value) }
        })
    }

    override fun trackOrsSearchSelection(resultPosition: ResultPositionBucket, resultCount: Int) {
        firebase.logEvent(EVENT_ORS_SEARCH_SELECTION, Bundle().apply {
            putString("result_position", resultPosition.value)
            putInt("result_count", resultCount.coerceIn(0, 10))
        })
    }

    override fun trackMapLayerSelected(baseMap: String, overlay: String) {
        val params = Bundle().apply {
            putString("base_map", baseMap)
            putString("overlay", overlay)
        }
        firebase.logEvent(EVENT_MAP_LAYER_SELECTED, params)
    }

    override fun trackMarkersImported(importedCount: Int, skippedCount: Int) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, AnalyticsNames.ContentType.MARKERS)
            putInt("imported_count", importedCount)
            putInt("skipped_count", skippedCount)
        }
        firebase.logEvent(EVENT_MARKERS_IMPORTED, params)
    }

    override fun trackMarkersExported(markerCount: Int) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, AnalyticsNames.ContentType.MARKERS)
            putInt("marker_count", markerCount)
        }
        firebase.logEvent(EVENT_MARKERS_EXPORTED, params)
    }

    override fun trackMarkersDeleted(markerCount: Int) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, AnalyticsNames.ContentType.MARKERS)
            putInt("marker_count", markerCount)
        }
        firebase.logEvent(EVENT_MARKERS_DELETED, params)
    }
}
