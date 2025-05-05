/*
 * Copyright 2024 - 2024 容均致 (harryrong@rushanio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.helper;

import java.io.Serializable;
import java.util.Objects;

/**
 * Coordinate system conversion related toolkits, mainstream coordinate systems include:
 * WGS84 coordinate system: colloquially Earth coordinate system, used by Maps outside China.
 * GCJ02 coordinate system: colloquially Mars coordinate system, used by Gaode, Tencent, Ali and so on.
 * <p>
 * Reference：<a href="https://en.wikipedia.org/wiki/Restrictions_on_geographic_data_in_China">...</a>
 * Reference: <a href="https://github.com/dromara/hutool">...</a>
 */
public final class CoordinateUtil {

    private CoordinateUtil() {

    }

    /**
     * Earth radius（Krasovsky 1940）
     */
    public static final double RADIUS = 6378245.0D;

    /**
     * Correction parameter (bias ee)
     */
    public static final double CORRECTION_PARAM = 0.00669342162296594323D;

    /**
     * Determine whether the coordinates are in non-China range
     * GCJ-02 coordinate system is only valid for China, no conversion is needed for non-Chinese areas.
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Whether the coordinates are in non-China area
     */
    public static boolean outOfChina(double latitude, double longitude) {
        return (latitude < 0.8293 || latitude > 55.8271) || (longitude < 72.004 || longitude > 137.8347);
    }

    /**
     * Convert WGS84 to GCJ-02
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @return GCJ-02 coordinate
     */
    public static Coordinate wgs84ToGcj02(double latitude, double longitude) {
        return new Coordinate(latitude, longitude).offset(offset(latitude, longitude, true));
    }

    /**
     * Convert GCJ-02 to WGS84
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @return WGS84 coordinate
     */
    public static Coordinate gcj02ToWgs84(double latitude, double longitude) {
        return new Coordinate(latitude, longitude).offset(offset(latitude, longitude, false));
    }

    /**
     * Offset algorithm for WGS84 to GCJ-02 conversion (not exact)
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @param isPlus Forward offset or not: WGS84 to GCJ-02 use forward, otherwise use reverse.
     * @return Offset coordinates
     */
    private static Coordinate offset(double latitude, double longitude, boolean isPlus) {
        double latitudeOffset = transformLatitude(latitude - 35.0, longitude - 105.0);
        double longitudeOffset = transformLongitude(latitude - 35.0, longitude - 105.0);

        double magic = Math.sin(latitude / 180.0 * Math.PI);
        magic = 1 - CORRECTION_PARAM * magic * magic;
        final double sqrtMagic = Math.sqrt(magic);

        latitudeOffset = (latitudeOffset * 180.0)
                / ((RADIUS * (1 - CORRECTION_PARAM)) / (magic * sqrtMagic) * Math.PI);
        longitudeOffset = (longitudeOffset * 180.0)
                / (RADIUS / sqrtMagic * Math.cos(latitude / 180.0 * Math.PI) * Math.PI);

        if (!isPlus) {
            latitudeOffset = -latitudeOffset;
            longitudeOffset = -longitudeOffset;
        }

        return new Coordinate(latitudeOffset, longitudeOffset);
    }

    /**
     * Calculate longitude coordinates
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @return ret Calculated
     */
    private static double transformLongitude(double latitude, double longitude) {
        double longitudeOffset = 300.0 + longitude + 2.0 * latitude
                + 0.1 * longitude * longitude + 0.1 * longitude * latitude
                + 0.1 * Math.sqrt(Math.abs(longitude));
        longitudeOffset += (20.0 * Math.sin(6.0 * longitude * Math.PI)
                + 20.0 * Math.sin(2.0 * longitude * Math.PI)) * 2.0 / 3.0;
        longitudeOffset += (20.0 * Math.sin(longitude * Math.PI)
                + 40.0 * Math.sin(longitude / 3.0 * Math.PI)) * 2.0 / 3.0;
        longitudeOffset += (150.0 * Math.sin(longitude / 12.0 * Math.PI)
                + 300.0 * Math.sin(longitude / 30.0 * Math.PI)) * 2.0 / 3.0;
        return longitudeOffset;
    }

    /**
     * Calculate latitude coordinates
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @return ret Calculated
     */
    private static double transformLatitude(double latitude, double longitude) {
        double longitudeOffset = -100.0 + 2.0 * longitude + 3.0 * latitude
                + 0.2 * latitude * latitude + 0.1 * longitude * latitude
                + 0.2 * Math.sqrt(Math.abs(longitude));
        longitudeOffset += (20.0 * Math.sin(6.0 * longitude * Math.PI)
                + 20.0 * Math.sin(2.0 * longitude * Math.PI)) * 2.0 / 3.0;
        longitudeOffset += (20.0 * Math.sin(latitude * Math.PI)
                + 40.0 * Math.sin(latitude / 3.0 * Math.PI)) * 2.0 / 3.0;
        longitudeOffset += (160.0 * Math.sin(latitude / 12.0 * Math.PI)
                + 320 * Math.sin(latitude * Math.PI / 30.0)) * 2.0 / 3.0;
        return longitudeOffset;
    }


    /**
     * Coordinate latitude and longitude
     */
    public static class Coordinate implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Latitude
         */
        private double latitude;

        /**
         * Longitude
         */
        private double longitude;

        /**
         * Constructor
         *
         * @param latitude Latitude
         * @param longitude Longitude
         */
        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        /**
         * Get latitude
         *
         * @return Latitude
         */
        public double getLatitude() {
            return latitude;
        }

        /**
         * Set latitude
         *
         * @param latitude Latitude
         * @return this
         */
        public Coordinate setLatitude(double latitude) {
            this.latitude = latitude;
            return this;
        }

        /**
         * Get longitude
         *
         * @return Longitude
         */
        public double getLongitude() {
            return longitude;
        }

        /**
         * Set longitude
         *
         * @param longitude Longitude
         * @return this
         */
        public Coordinate setLongitude(double longitude) {
            this.longitude = longitude;
            return this;
        }

        /**
         * Offset the current coordinates by the specified coordinates.
         *
         * @param offset offset
         * @return this
         */
        public Coordinate offset(Coordinate offset) {
            this.latitude += offset.latitude;
            this.longitude += offset.longitude;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Coordinate that = (Coordinate) o;
            return Double.compare(that.latitude, latitude) == 0 && Double.compare(that.longitude, longitude) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(latitude, longitude);
        }

        @Override
        public String toString() {
            return "Coordinate{"
                    + "latitude=" + latitude
                    + ", longitude=" + longitude
                    + '}';
        }
    }
}
