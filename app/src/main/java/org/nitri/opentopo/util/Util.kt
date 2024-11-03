package org.nitri.opentopo.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.os.Environment
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.Point
import io.ticofab.androidgpxparser.parser.domain.WayPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.io.File
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

object Util {
    const val NO_ELEVATION_VALUE = -99999

    /**
     * Get GPX bounds
     *
     * @param gpx Gpx
     * @return BoundingBox
     */
    fun area(gpx: Gpx?): BoundingBox {
        return area(getAllTrackGeoPoints(gpx))
    }

    /**
     * Get geo points bounds
     *
     * @param points List<GeoPoint>
     * @return BoundingBox
    </GeoPoint> */
    private fun area(points: List<GeoPoint?>): BoundingBox {
        var north = Double.MIN_VALUE
        var south = Double.MAX_VALUE
        var west = Double.MAX_VALUE
        var east = Double.MIN_VALUE

        points.forEach { point ->
            point?.let {
                if (it.latitude > north) north = it.latitude
                if (it.latitude < south) south = it.latitude
                if (it.longitude < west) west = it.longitude
                if (it.longitude > east) east = it.longitude
            }
        }

        return BoundingBox(north, east, south, west)
    }

    private fun getAllTrackGeoPoints(gpx: Gpx?): List<GeoPoint?> {
        val geoPoints: MutableList<GeoPoint?> = ArrayList()
        if (gpx != null && gpx.tracks != null) {
            for (track in gpx.tracks) {
                if (track.trackSegments != null) {
                    for (segment in track.trackSegments) {
                        if (segment.trackPoints != null) {
                            for (trackPoint in segment.trackPoints) {
                                geoPoints.add(GeoPoint(trackPoint.latitude, trackPoint.longitude))
                            }
                        }
                    }
                }
            }
        }
        return geoPoints
    }

    /**
     * Get way point types (categories) from GPX
     *
     * @param gpx
     * @param defaultType
     * @return
     */
    fun getWayPointTypes(gpx: Gpx, defaultType: String): List<String> {
        val types: MutableList<String> = ArrayList()
        if (gpx.wayPoints != null) {
            for (wayPoint in gpx.wayPoints) {
                var type = defaultType
                if (!TextUtils.isEmpty(wayPoint.type)) type = wayPoint.type
                if (!types.contains(type)) types.add(type)
            }
        }
        types.sort()
        return types
    }

    /**
     * Get a list of way points by type (categpry)
     *
     * @param gpx
     * @param type
     * @return
     */
    fun getWayPointsByType(gpx: Gpx, type: String?): List<WayPoint> {
        val wayPoints: MutableList<WayPoint> = ArrayList()
        if (gpx.wayPoints != null) {
            for (wayPoint in gpx.wayPoints) {
                if (!TextUtils.isEmpty(wayPoint.type) && wayPoint.type == type) wayPoints.add(
                    wayPoint
                ) else if (TextUtils.isEmpty(wayPoint.type) && TextUtils.isEmpty(type)) wayPoints.add(
                    wayPoint
                )
            }
        }
        return wayPoints
    }

    private fun resolveThemeAttr(context: Context, @AttrRes attrRes: Int): TypedValue {
        val theme = context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue
    }

    /**
     * Get color integer by attribute
     *
     * @param context
     * @param colorAttr
     * @return
     */
    @ColorInt
    fun resolveColorAttr(context: Context, @AttrRes colorAttr: Int): Int {
        val resolvedAttr = resolveThemeAttr(context, colorAttr)
        // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
        val colorRes =
            if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
        return ContextCompat.getColor(context, colorRes)
    }

    /**
     * Spanned text from HTML (compat)
     *
     * @param source
     * @return
     */
    @JvmStatic
    @Suppress("deprecation")
    fun fromHtml(source: String?): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(source)
        }
    }

    /**
     * Antenna altitude above mean sea level
     *
     * @param nmea NMEA
     * @return antenna altitude
     */
    @JvmStatic
    fun elevationFromNmea(nmea: String): Double {
        if (!TextUtils.isEmpty(nmea) && nmea.startsWith("\$GPGGA")) {
            val tokens = nmea.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                val elevation = tokens[9]
                if (!TextUtils.isEmpty(elevation)) {
                    return elevation.toDouble()
                }
            } catch (ex: Exception) {
                Log.e(
                    "NMEA", "elevationFromNmea: "
                            + ex.message
                )
                ex.printStackTrace()
            }
        }
        return NO_ELEVATION_VALUE.toDouble()
    }

    /**
     * Bitmap from vector drawable
     *
     * @param context
     * @param drawableId
     * @return
     */
    fun getBitmapFromDrawable(context: Context?, @DrawableRes drawableId: Int, alpha: Int): Bitmap {
        if (context == null) throw IllegalArgumentException("Context cannot be null")
        val drawable = AppCompatResources.getDrawable(context, drawableId)
        drawable?.alpha = alpha

        return when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            is VectorDrawableCompat, is VectorDrawable -> createBitmap(drawable)
            null -> Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Return a default tiny transparent bitmap
            else -> throw IllegalArgumentException("Unsupported drawable type")
        }
    }

    private fun createBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun getOsmdroidBasePath(context: Context, externalStorage: Boolean) : File {
        return if (externalStorage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "osmdroid"
                )
            } else {
                File(Environment.getExternalStorageDirectory().toString() + "/Download/osmdroid");
            }
        } else {
            File(context.cacheDir.absolutePath, "osmdroid")
        }
    }

    fun getAppName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) {
            applicationInfo.nonLocalizedLabel.toString()
        } else {
            context.getString(stringId)
        }
    }

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "N/A"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "N/A"
        }
    }
}
