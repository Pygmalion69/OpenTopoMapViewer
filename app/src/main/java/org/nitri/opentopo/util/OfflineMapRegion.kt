package org.nitri.opentopo.util

/** Deliberately coarse visibility hint; this does not restrict downloads or imports. */
internal object OfflineMapRegion {
    fun isInEurope(latitude: Double?, longitude: Double?, timeZoneId: String): Boolean {
        if (latitude != null && longitude != null &&
            latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
        ) {
            // Includes Iceland, the Azores, Madeira, the Canaries and Svalbard.
            // A rectangle intentionally also includes some neighbouring areas.
            return latitude in 27.0..81.0 && longitude in -32.0..45.0
        }
        return timeZoneId.startsWith("Europe/") || timeZoneId in setOf(
            "Atlantic/Azores", "Atlantic/Canary", "Atlantic/Faroe", "Atlantic/Faeroe",
            "Atlantic/Madeira", "Atlantic/Reykjavik", "Arctic/Longyearbyen",
            "Asia/Istanbul", "Asia/Nicosia", "Asia/Famagusta", "Iceland", "Portugal"
        )
    }
}
