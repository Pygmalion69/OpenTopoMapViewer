package org.nitri.opentopo.domain;

/**
 * GPX point on distance line
 *
 * Note: io.ticofab.androidgpxparser.parser.domain.Point is package private
 */
public class DistancePoint {

    private final double mDistance;
    private final double mElevation;

    DistancePoint(Builder builder) {
        mDistance = builder.mDistance;
        mElevation = builder.mElevation;
    }

    /**
     * @return the distance in meters
     */
    public double getDistance() {
        return mDistance;
    }

    /**
     * @return the elevation in meters
     */
    public double getElevation() {
        return mElevation;
    }

    public static class Builder {

        private double mDistance;
        private double mElevation;

        public Builder setDistance(double distance) {
            mDistance = distance;
            return this;
        }

        public Builder setElevation(double elevation) {
            mElevation = elevation;
            return this;
        }

        public DistancePoint build() {
            return new DistancePoint(this);
        }
    }

}
