package org.nitri.opentopo

/** Viewport restoration policy, independent of Android lifecycle and location permissions. */
internal data class MapViewport(val latitude: Double, val longitude: Double, val zoom: Double) {
    fun clampedZoom(minimum: Double, maximum: Double): Double =
        (zoom.takeIf { it.isFinite() } ?: DEFAULT_ZOOM).coerceIn(minimum, maximum)

    companion object {
        const val DEFAULT_ZOOM = 15.0
        val FALLBACK = MapViewport(51.78962, 6.14120, DEFAULT_ZOOM)

        fun restore(latitude: Double?, longitude: Double?, zoom: Double): MapViewport? {
            if (latitude == null || longitude == null ||
                !latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
            return MapViewport(latitude, longitude, zoom)
        }

        fun fromPreferences(values: Map<String, *>): MapViewport? = restore(
            (values["latitude"] as? Float)?.toDouble(),
            (values["longitude"] as? Float)?.toDouble(),
            (values["zoom"] as? Float)?.toDouble() ?: DEFAULT_ZOOM
        )

        fun select(restored: MapViewport?, persisted: MapViewport?, known: MapViewport?): MapViewport =
            restored ?: persisted ?: known ?: FALLBACK
    }
}
