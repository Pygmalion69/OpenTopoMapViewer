package org.nitri.ors.client

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import org.nitri.ors.api.OpenRouteServiceApi
import okhttp3.MediaType.Companion.toMediaType

object OpenRouteServiceClient {
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
                val originalUrl = original.url

                val newUrl = originalUrl.newBuilder()
                    .addQueryParameter("api_key", apiKey)
                    .build()

                val newRequest = original.newBuilder()
                    .url(newUrl)
                    .addHeader("User-Agent", userAgent)
                    .build()

                chain.proceed(newRequest)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create()
    }
}

