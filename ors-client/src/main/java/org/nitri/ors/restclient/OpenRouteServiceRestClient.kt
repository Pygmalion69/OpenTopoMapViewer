package org.nitri.ors.restclient

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import org.nitri.ors.api.OpenRouteServiceApi
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

object OpenRouteServiceRestClient {
    fun create(apiKey: String, context: Context): OpenRouteServiceApi {

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = packageInfo.versionName ?: "unknown"
        val userAgent = "${packageInfo.packageName}/$appVersion (ORS-Android-Client)"

        val json = Json { ignoreUnknownKeys = true }

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                val original = chain.request()

                val newRequest = original.newBuilder()
                    // ORS expects the API key in the Authorization header (no Bearer prefix)
                    .addHeader("Authorization", apiKey)
                    .addHeader("User-Agent", userAgent)
                    .addHeader("Accept", "application/json, application/geo+json, application/xml, text/xml, application/gpx+xml")
                    .build()

                chain.proceed(newRequest)
            }
            // Increase timeouts to accommodate heavier endpoints like POIs
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .client(client)
            // Prefer application/json for requests; also support application/geo+json responses
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addConverterFactory(json.asConverterFactory("application/geo+json".toMediaType()))
            .build()

        return retrofit.create()
    }
}

