package org.nitri.opentopo.nearby

import android.annotation.SuppressLint
import android.content.Context
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient
import org.nitri.opentopo.BuildConfig

internal fun normalizeNearbyThumbnailUrl(url: String?): String? = when {
    url == null -> null
    url.startsWith("http://") -> url.replaceFirst("http://", "https://")
    url.startsWith("//") -> "https:$url"
    else -> url
}

/** Application-scoped loader: rows and recreated fragment views share its client/cache. */
internal object NearbyImages {
    // Picasso receives only applicationContext below; no Activity or View is retained.
    @SuppressLint("StaticFieldLeak")
    private var instance: Picasso? = null

    @Synchronized
    fun getPicasso(context: Context): Picasso = instance ?: run {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "OpenTopoMapViewer/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})")
                    .header("Accept", "image/*")
                    .header("Referer", "https://www.wikipedia.org/")
                    .build()
                chain.proceed(request)
            }
            .build()
        Picasso.Builder(context.applicationContext)
            .downloader(OkHttp3Downloader(client))
            .build()
            .also { instance = it }
    }
}
