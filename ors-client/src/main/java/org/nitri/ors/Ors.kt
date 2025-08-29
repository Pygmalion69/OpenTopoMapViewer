package org.nitri.ors

import android.content.Context

object Ors {
    /** If you already construct the Retrofit API elsewhere, inject it here. */
    @JvmStatic
    fun create(apiKey: String, context: Context): OrsClient = DefaultOrsClient(apiKey, context)
}