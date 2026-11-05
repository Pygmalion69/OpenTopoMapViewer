package org.nitri.opentopo.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import java.util.TimeZone
import java.util.concurrent.TimeUnit

internal object OfflineMapLinks {
    const val CATALOGUE_URL = "https://opentopomap.nitri.org/offline/"
    const val REQUEST_URL = "https://github.com/Pygmalion69/OpenTopoMapViewer/issues/new?template=offline_map_request.md"

    fun isInEurope(context: Context): Boolean {
        val hasLocationPermission = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

        // Reuse recent cached fixes only. Never start GPS, request permission or use IP geolocation.
        val location = if (hasLocationPermission) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            try {
                manager?.getProviders(false)?.mapNotNull { provider ->
                    try {
                        manager.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }?.filter {
                    val age = SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos
                    age in 0..TimeUnit.HOURS.toNanos(24)
                }?.maxByOrNull { it.elapsedRealtimeNanos }
            } catch (_: SecurityException) {
                null
            }
        } else null

        return OfflineMapRegion.isInEurope(
            location?.latitude, location?.longitude, TimeZone.getDefault().id
        )
    }
}
