/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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
 * Reference: <a href="https://github.com/dromara/hutool/blob/v5-master/hutool-core/src/main/java/cn/hutool/core/util/CoordinateUtil.java">...</a>
 */
public class CoordinateUtil {

    /**
     * Coordinate conversion parameters：π（Pi)
     */
    public static final double PI = 3.1415926535897932384626433832795D;

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
     * @param longitude Longitude
     * @param latitude Latitude
     * @return Whether the coordinates are in non-China area
     */
    public static boolean outOfChina(double longitude, double latitude) {
        return (longitude < 72.004 || longitude > 137.8347) || (latitude < 0.8293 || latitude > 55.8271);
    }

    /**
     * Convert WGS84 to GCJ-02
     *
     * @param longitude Longitude
     * @param latitude Latitude
     * @return GCJ-02 coordinate
     */
    public static Coordinate wgs84ToGcj02(double longitude, double latitude) {
        return new Coordinate(longitude, latitude).offset(offset(longitude, latitude, true));
    }

    /**
     * Convert GCJ-02 to WGS84
     *
     * @param longitude Longitude
     * @param latitude Latitude
     * @return WGS84 coordinate
     */
    public static Coordinate gcj02ToWgs84(double longitude, double latitude) {
        return new Coordinate(longitude, latitude).offset(offset(longitude, latitude, false));
    }

    /**
     * Offset algorithm for WGS84 to GCJ-02 conversion (not exact)
     *
     * @param longitude Longitude
     * @param latitude Latitude
     * @param isPlus Forward offset or not: WGS84 to GCJ-02 use forward, otherwise use reverse.
     * @return Offset coordinates
     */
    private static Coordinate offset(double longitude, double latitude, boolean isPlus) {
        double dlng = transLng(longitude - 105.0, latitude - 35.0);
        double dlat = transLat(longitude - 105.0, latitude - 35.0);

        double magic = Math.sin(latitude / 180.0 * PI);
        magic = 1 - CORRECTION_PARAM * magic * magic;
        final double sqrtMagic = Math.sqrt(magic);

        dlng = (dlng * 180.0) / (RADIUS / sqrtMagic * Math.cos(latitude / 180.0 * PI) * PI);
        dlat = (dlat * 180.0) / ((RADIUS * (1 - CORRECTION_PARAM)) / (magic * sqrtMagic) * PI);

        if(false == isPlus){
            dlng = - dlng;
            dlat = - dlat;
        }

        return new Coordinate(dlng, dlat);
    }

    /**
     * Calculate longitude coordinates
     *
     * @param longitude Longitude
     * @param latitude Latitude
     * @return ret Calculated
     */
    private static double transLng(double longitude, double latitude) {
        double ret = 300.0 + longitude + 2.0 * latitude + 0.1 * longitude * longitude + 0.1 * longitude * latitude + 0.1 * Math.sqrt(Math.abs(longitude));
        ret += (20.0 * Math.sin(6.0 * longitude * PI) + 20.0 * Math.sin(2.0 * longitude * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(longitude * PI) + 40.0 * Math.sin(longitude / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(longitude / 12.0 * PI) + 300.0 * Math.sin(longitude / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * Calculate latitude coordinates
     *
     * @param longitude Longitude
     * @param latitude Latitude
     * @return ret Calculated
     */
    private static double transLat(double longitude, double latitude) {
        double ret = -100.0 + 2.0 * longitude + 3.0 * latitude + 0.2 * latitude * latitude + 0.1 * longitude * latitude
                + 0.2 * Math.sqrt(Math.abs(longitude));
        ret += (20.0 * Math.sin(6.0 * longitude * PI) + 20.0 * Math.sin(2.0 * longitude * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(latitude * PI) + 40.0 * Math.sin(latitude / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(latitude / 12.0 * PI) + 320 * Math.sin(latitude * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }


    /**
     * Coordinate latitude and longitude
     */
    public static class Coordinate implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Longitude
         */
        private double longitude;
        /**
         * Latitude
         */
        private double latitude;

        /**
         * Constructor
         *
         * @param longitude Longitude
         * @param latitude Latitude
         */
        public Coordinate(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
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
         * Offset the current coordinates by the specified coordinates.
         *
         * @param offset offset
         * @return this
         */
        public Coordinate offset(Coordinate offset){
            this.longitude += offset.longitude;
            this.latitude += offset.latitude;
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
            return Double.compare(that.longitude, longitude) == 0 && Double.compare(that.latitude, latitude) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(longitude, latitude);
        }

        @Override
        public String toString() {
            return "Coordinate{" +
                    "longitude=" + longitude +
                    ", latitude=" + latitude +
                    '}';
        }
    }
}
