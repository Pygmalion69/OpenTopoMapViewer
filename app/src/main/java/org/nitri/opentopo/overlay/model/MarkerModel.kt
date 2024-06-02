package org.nitri.opentopo.overlay.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MarkerModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var seq: Int,
    var latitude: Double,
    var longitude: Double,
    var name: String,
    var description: String,
    var nearbyId: Int = 0
)
