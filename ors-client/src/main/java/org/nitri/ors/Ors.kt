package org.nitri.ors

import android.content.Context

/**
 * Factory for obtaining [OrsClient] instances.
 */
object Ors {
    /**
     * Creates a default [OrsClient] backed by Retrofit.
     *
     * @param apiKey API key obtained from openrouteservice.org
     * @param context Android context used for HTTP client construction
     */
    @JvmStatic
    fun create(apiKey: String, context: Context): OrsClient = DefaultOrsClient(apiKey, context)
}