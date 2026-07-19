package org.nitri.opentopo.overlay

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.nitri.opentopo.da.MarkerDao
import org.nitri.opentopo.model.MarkerModel

@Database(entities = [MarkerModel::class], version = 4, exportSchema = true)
abstract class OverlayDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao

    companion object {
        @Volatile
        private var INSTANCE: OverlayDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE MarkerModel ADD COLUMN color INTEGER NOT NULL DEFAULT -859029708")
            }
        }

        fun getDatabase(context: Context): OverlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OverlayDatabase::class.java,
                    "overlay_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
