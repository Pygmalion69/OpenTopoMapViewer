package org.nitri.opentopo

import android.content.SharedPreferences
import androidx.core.content.edit

/** One-time provider-requested switch; later manual selections must be preserved. */
internal object OpenTopoMapSourceMigration {
    const val COMPLETED = "otm_official_default_migration_v1_done"
    const val NOTICE_PENDING = "otm_official_default_migration_v1_notice_pending"

    fun run(preferences: SharedPreferences) {
        if (preferences.getBoolean(COMPLETED, false)) return
        val source = preferences.getString("open_topo_map_source", null)
        preferences.edit {
            if (source == "opentopomap_r") {
                putString("open_topo_map_source", "opentopomap")
                putBoolean(NOTICE_PENDING, true)
            }
            putBoolean(COMPLETED, true)
        }
    }
}
