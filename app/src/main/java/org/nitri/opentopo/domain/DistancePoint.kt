package org.nitri.opentopo.domain

/**
 * GPX point on distance line
 *
 * Note: io.ticofab.androidgpxparser.parser.domain.Point is package private
 */
class DistancePoint internal constructor(builder: Builder) {
    /**
     * @return the distance in meters
     */
    val distance: Double?

    /**
     * @return the elevation in meters
     */
    val elevation: Double?

    init {
        distance = builder.mDistance
        elevation = builder.mElevation
    }

    class Builder {
        var mDistance: Double? = null
        var mElevation: Double? = null
        fun setDistance(distance: Double?): Builder {
            mDistance = distance
            return this
        }

        fun setElevation(elevation: Double?): Builder {
            mElevation = elevation
            return this
        }

        fun build(): DistancePoint {
            return DistancePoint(this)
        }
    }
}
