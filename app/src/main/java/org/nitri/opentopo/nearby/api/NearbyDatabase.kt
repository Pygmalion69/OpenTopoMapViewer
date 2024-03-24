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
        private var instance: NearbyDatabase? = null
        @JvmStatic
        @Synchronized
        fun getDatabase(context: Context): NearbyDatabase? {
            if (instance == null) {
                instance = databaseBuilder(
                    context.applicationContext,
                    NearbyDatabase::class.java,
                    "nearby-database"
                )
                    .build()
            }
            return instance
        }
    }
}
