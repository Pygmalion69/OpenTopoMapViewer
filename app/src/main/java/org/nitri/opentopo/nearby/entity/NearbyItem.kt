package org.nitri.opentopo.nearby.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Nearby", indices = [Index(value = arrayOf("pageid"), unique = true)])
class NearbyItem : Comparable<NearbyItem> {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    var id = 0
    @JvmField
    var pageid: String? = null
    @JvmField
    var index = 0
    @JvmField
    var title: String? = null
    @JvmField
    var description: String? = null
    @JvmField
    var thumbnail: String? = null
    @JvmField
    var width = 0
    @JvmField
    var height = 0
    @JvmField
    var lat = 0.0
    @JvmField
    var lon = 0.0
    @JvmField
    var url: String? = null

    @JvmField
    @Ignore
    var distance = 0
    override fun compareTo(other: NearbyItem): Int {
        return Integer.compare(distance, other.distance)
    }
}
