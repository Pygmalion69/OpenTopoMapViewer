package org.nitri.opentopo.util

import android.content.Context
import androidx.preference.PreferenceManager

/** UUID, visibility and opacity are independent; hiding never discards the selection. */
internal class LocalMapPreferences(private val prefs: android.content.SharedPreferences) {
    constructor(context: Context) : this(PreferenceManager.getDefaultSharedPreferences(context))
    val id get() = prefs.getString("local_map_id", null)
    val visible get() = prefs.getBoolean("local_map_visible", false)
    val opacity get() = prefs.getInt("local_map_opacity", 65).coerceIn(0, 100)
    val navigation get() = prefs.getString("local_map_navigation", null)
    fun select(id: String, navigate: Boolean = false) {
        prefs.edit().putString("local_map_id", id).putBoolean("local_map_visible", true)
            .putString("local_map_navigation", if (navigate) id else null).apply()
    }
    fun show(visible: Boolean) { prefs.edit().putBoolean("local_map_visible", visible).apply() }
    fun opacity(value: Int) { prefs.edit().putInt("local_map_opacity", value.coerceIn(0, 100)).apply() }
    fun consumeNavigation() { prefs.edit().remove("local_map_navigation").apply() }
    fun clear() { prefs.edit().remove("local_map_id").remove("local_map_navigation")
        .putBoolean("local_map_visible", false).apply() }
}
