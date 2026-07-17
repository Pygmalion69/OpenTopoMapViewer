package org.nitri.opentopo.ors

import org.nitri.ors.OrsClient
import org.nitri.ors.helper.ElevationHelper

class Elevation(
    private val client: OrsClient
) {
    private val elevationHelper = ElevationHelper()

    suspend fun getPointElevation(
        longitude: Double,
        latitude: Double
    ): Double {
        val response = with(elevationHelper) {
            client.getElevationPoint(
                lon = longitude,
                lat = latitude,
                formatOut = "geojson"
            )
        }

        val elevation = response.geometry.coordinates.getOrNull(2)

        require(elevation != null && elevation.isFinite()) {
            "ORS elevation response does not contain a valid elevation"
        }

        return elevation
    }
}
