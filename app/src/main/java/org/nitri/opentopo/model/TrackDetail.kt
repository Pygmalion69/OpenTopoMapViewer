package org.nitri.opentopo.model

import org.nitri.opentopo.domain.DistancePoint
import java.util.Collections

class TrackDetail(val name: String, distancePoints: List<DistancePoint>?) {
    val distancePoints: List<DistancePoint>

    init {
        this.distancePoints = Collections.unmodifiableList(distancePoints)
    }
}
