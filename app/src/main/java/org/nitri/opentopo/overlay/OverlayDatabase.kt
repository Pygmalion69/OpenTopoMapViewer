package org.nitri.opentopo.overlay

import androidx.room.Database
import androidx.room.RoomDatabase
import org.nitri.opentopo.overlay.da.MarkerDao
import org.nitri.opentopo.overlay.model.MarkerModel

@Database(entities = [MarkerModel::class], version = 1)
abstract class OverlayDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
}