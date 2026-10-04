package org.nitri.opentopo

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
class OpenTopoMapSourceMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("otm_migration_test", Context.MODE_PRIVATE)
    private val sourceKey = "open_topo_map_source"

    @Before fun reset() { prefs.edit().clear().commit() }
    @After fun cleanUp() { prefs.edit().clear().commit() }

    @Test fun savedRIsMigratedAtomicallyAndOtherPreferencesSurvive() {
        prefs.edit().putString(sourceKey, "opentopomap_r")
            .putInt("base_map", 1).putString("unrelated", "keep").commit()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, _ ->
            assertEquals("opentopomap", preferences.getString(sourceKey, null))
            assertTrue(preferences.getBoolean(OpenTopoMapSourceMigration.COMPLETED, false))
            assertTrue(preferences.getBoolean(OpenTopoMapSourceMigration.NOTICE_PENDING, false))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                OpenTopoMapSourceMigration.run(prefs)
            }
        } finally {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
        assertEquals("opentopomap", prefs.getString(sourceKey, null))
        assertTrue(prefs.getBoolean(OpenTopoMapSourceMigration.COMPLETED, false))
        assertTrue(prefs.getBoolean(OpenTopoMapSourceMigration.NOTICE_PENDING, false))
        assertEquals(1, prefs.getInt("base_map", -1))
        assertEquals("keep", prefs.getString("unrelated", null))
    }

    @Test fun otherSourcesAreUnchangedWithoutNotice() {
        for (source in listOf("opentopomap", "top_o_map")) {
            reset()
            prefs.edit().putString(sourceKey, source).commit()
            OpenTopoMapSourceMigration.run(prefs)
            assertEquals(source, prefs.getString(sourceKey, null))
            assertTrue(prefs.getBoolean(OpenTopoMapSourceMigration.COMPLETED, false))
            assertFalse(prefs.getBoolean(OpenTopoMapSourceMigration.NOTICE_PENDING, false))
        }
    }

    @Test fun missingSourceUsesOfficialRuntimeDefaultWithoutNotice() {
        OpenTopoMapSourceMigration.run(prefs)
        assertFalse(prefs.contains(sourceKey))
        assertEquals("opentopomap", MapFragment().readOpenTopoMapSource(prefs))
        assertTrue(prefs.getBoolean(OpenTopoMapSourceMigration.COMPLETED, false))
        assertFalse(prefs.getBoolean(OpenTopoMapSourceMigration.NOTICE_PENDING, false))
    }

    @Test fun nullRuntimeValueAlsoUsesOfficialDefault() {
        val nullPreferences = mock(SharedPreferences::class.java)
        `when`(nullPreferences.getString(sourceKey, "opentopomap")).thenReturn(null)
        assertEquals("opentopomap", MapFragment().readOpenTopoMapSource(nullPreferences))
    }

    @Test fun migrationIsIdempotentAndLaterRSelectionSurvives() {
        prefs.edit().putString(sourceKey, "opentopomap_r").commit()
        OpenTopoMapSourceMigration.run(prefs)
        val migrated = prefs.all
        OpenTopoMapSourceMigration.run(prefs)
        assertEquals(migrated, prefs.all)
        prefs.edit().putString(sourceKey, "opentopomap_r")
            .putBoolean(OpenTopoMapSourceMigration.NOTICE_PENDING, false).commit()
        val selectedAgain = prefs.all
        repeat(3) { OpenTopoMapSourceMigration.run(prefs) }
        assertEquals(selectedAgain, prefs.all)
    }
}
