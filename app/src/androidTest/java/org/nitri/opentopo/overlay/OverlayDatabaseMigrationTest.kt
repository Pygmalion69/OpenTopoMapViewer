package org.nitri.opentopo.overlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.opentopo.ui.color.DEFAULT_MARKER_COLOR

@RunWith(AndroidJUnit4::class)
class OverlayDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @Test
    fun migrate3To4Manual() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
        
        val db3 = context.openOrCreateDatabase(TEST_DB, 0, null)
        db3.execSQL("""
            CREATE TABLE IF NOT EXISTS `MarkerModel` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `seq` INTEGER NOT NULL, 
                `latitude` REAL NOT NULL, 
                `longitude` REAL NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `nearbyId` INTEGER NOT NULL, 
                `routeWaypoint` INTEGER NOT NULL
            )
        """.trimIndent())
        
        db3.execSQL("INSERT INTO MarkerModel (seq, latitude, longitude, name, description, nearbyId, routeWaypoint) VALUES (1, 10.0, 20.0, 'Test Marker', 'Test Description', 0, 0)")
        db3.close()

        // Use a SupportSQLiteDatabase for the migration as Room Migrations expect it
        val db4 = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        ).writableDatabase
        
        OverlayDatabase.MIGRATION_3_4.migrate(db4)

        // Verify
        val cursor = db4.query("SELECT * FROM MarkerModel WHERE id = 1")
        cursor.moveToFirst()
        assertEquals("Test Marker", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals(DEFAULT_MARKER_COLOR, cursor.getInt(cursor.getColumnIndex("color")))
        cursor.close()
        db4.close()
        
        context.deleteDatabase(TEST_DB)
    }
}
