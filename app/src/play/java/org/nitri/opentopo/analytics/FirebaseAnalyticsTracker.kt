package org.nitri.opentopo.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.ticofab.androidgpxparser.parser.domain.Gpx

private const val EVENT_GPX_LOADED = "gpx_loaded"
private const val EVENT_KML_LOADED = "kml_loaded"
private const val EVENT_ROUTE_CALCULATED = "route_calculated"
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
            fileName?.let { putString("file_name", it) }
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
            fileName?.let { putString("file_name", it) }
        }
        firebase.logEvent(EVENT_KML_LOADED, params)
    }

    override fun trackRouteCalculated(profile: String?, waypointCount: Int) {
        val params = Bundle().apply {
            profile?.let { putString("profile", it) }
            putInt("waypoint_count", waypointCount)
        }
        firebase.logEvent(EVENT_ROUTE_CALCULATED, params)
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
