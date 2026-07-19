package org.nitri.opentopo.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.nitri.opentopo.ui.color.DEFAULT_MARKER_COLOR

@Entity
data class MarkerModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var seq: Int,
    var latitude: Double,
    var longitude: Double,
    var name: String,
    var description: String,
    var nearbyId: Int = 0,
    var routeWaypoint: Boolean = false,
    var color: Int = DEFAULT_MARKER_COLOR
)
