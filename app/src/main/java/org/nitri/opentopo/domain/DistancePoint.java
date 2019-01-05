package org.nitri.opentopo.domain;

/**
 * GPX point on distance line
 *
 * Note: io.ticofab.androidgpxparser.parser.domain.Point is package private
 */
public class DistancePoint {

    private final Double mDistance;
    private final Double mElevation;

    DistancePoint(Builder builder) {
        mDistance = builder.mDistance;
        mElevation = builder.mElevation;
    }

    /**
     * @return the distance in meters
     */
    public Double getDistance() {
        return mDistance;
    }

    /**
     * @return the elevation in meters
     */
    public Double getElevation() {
        return mElevation;
    }

    public static class Builder {

        private Double mDistance;
        private Double mElevation;

        public Builder setDistance(Double distance) {
            mDistance = distance;
            return this;
        }

        public Builder setElevation(Double elevation) {
            mElevation = elevation;
            return this;
        }

        public DistancePoint build() {
            return new DistancePoint(this);
        }
    }

}
