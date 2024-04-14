package org.nitri.opentopo.nearby.api

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import org.nitri.opentopo.nearby.da.NearbyDao
import org.nitri.opentopo.nearby.entity.NearbyItem

@Database(entities = [NearbyItem::class], version = 1, exportSchema = false)
abstract class NearbyDatabase : RoomDatabase() {
    abstract fun nearbyDao(): NearbyDao?

    companion object {
        @Volatile
        private var instance: NearbyDatabase? = null

        fun getDatabase(context: Context): NearbyDatabase {
            return instance ?: synchronized(this) {
                // Double-check in case 'instance' was initialized while this thread was blocked
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): NearbyDatabase {
            return databaseBuilder(
                context.applicationContext,
                NearbyDatabase::class.java,
                "nearby-database"
            ).build()
        }
    }
}
