package org.nitri.opentopo.overlay

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.nitri.opentopo.overlay.da.MarkerDao
import org.nitri.opentopo.overlay.model.MarkerModel

@Database(entities = [MarkerModel::class], version = 3, exportSchema = false)
abstract class OverlayDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao

    companion object {
        @Volatile
        private var INSTANCE: OverlayDatabase? = null

        fun getDatabase(context: Context): OverlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OverlayDatabase::class.java,
                    "overlay_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}