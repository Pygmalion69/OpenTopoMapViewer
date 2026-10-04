package org.nitri.opentopo

import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.view.OpenTopoMapMigrationDialog

@RunWith(AndroidJUnit4::class)
class OpenTopoMapMigrationNoticeTest {
    @Test fun startupMigratesAndNoticeSurvivesRecreationUntilDismissed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val keys = listOf("open_topo_map_source", OpenTopoMapSourceMigration.COMPLETED,
            OpenTopoMapSourceMigration.NOTICE_PENDING)
        val original = prefs.all.filterKeys { it in keys }
        try {
            prefs.edit().putString(keys[0], "opentopomap_r")
                .remove(keys[1]).remove(keys[2]).commit()
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals("opentopomap", prefs.getString(keys[0], null))
                    assertEquals(1, activity.supportFragmentManager.fragments
                        .filterIsInstance<OpenTopoMapMigrationDialog>().size)
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val notices = activity.supportFragmentManager.fragments
                        .filterIsInstance<OpenTopoMapMigrationDialog>()
                    assertEquals(1, notices.size)
                    assertTrue(prefs.getBoolean(keys[2], false))
                    (notices.single().requireDialog() as AlertDialog)
                        .getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                }
                // AlertDialog dispatches its button listener through the main queue.
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                assertFalse(prefs.getBoolean(keys[2], false))
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    assertTrue(activity.supportFragmentManager.fragments
                        .filterIsInstance<OpenTopoMapMigrationDialog>().isEmpty())
                }
            }
        } finally {
            prefs.edit().apply {
                keys.forEach { remove(it) }
                original.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                    }
                }
            }.commit()
        }
    }
}
