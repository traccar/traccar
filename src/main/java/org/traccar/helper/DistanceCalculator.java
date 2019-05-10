/*
 * Copyright 2014 - 2016 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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

public final class DistanceCalculator {

    private DistanceCalculator() {
    }

    private static final double EQUATORIAL_EARTH_RADIUS = 6378.1370;
    private static final double DEG_TO_RAD = Math.PI / 180;

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dlong = (lon2 - lon1) * DEG_TO_RAD;
        double dlat = (lat2 - lat1) * DEG_TO_RAD;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1 * DEG_TO_RAD) * Math.cos(lat2 * DEG_TO_RAD) * Math.pow(Math.sin(dlong / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = EQUATORIAL_EARTH_RADIUS * c;
        return d * 1000;
    }

    public static double distanceToLine(
            double pointLat, double pointLon, double lat1, double lon1, double lat2, double lon2) {
        double d0 = distance(pointLat, pointLon, lat1, lon1);
        double d1 = distance(lat1, lon1, lat2, lon2);
        double d2 = distance(lat2, lon2, pointLat, pointLon);
        if (Math.pow(d0, 2) > Math.pow(d1, 2) + Math.pow(d2, 2)) {
            return d2;
        }
        if (Math.pow(d2, 2) > Math.pow(d1, 2) + Math.pow(d0, 2)) {
            return d0;
        }
        double halfP = (d0 + d1 + d2) * 0.5;
        double area = Math.sqrt(halfP * (halfP - d0) * (halfP - d1) * (halfP - d2));
        return 2 * area / d1;
    }

    public static boolean lineCrossesLine(
            double aLat1, double aLon1, double aLat2, double aLon2,
            double bLat1, double bLon1, double bLat2, double bLon2) {

        double det, gamma, lambda;
        det = (aLat2 - aLat1) * (bLon2 - bLon1) - (bLat2 - bLat1) * (aLon2 - aLon1);

        if (det == 0) {
            return false;
        } else {
            lambda = ((bLon2 - bLon1) * (bLat2 - aLat1) + (bLat1 - bLat2) * (bLon2 - aLon1)) / det;
            gamma = ((aLon1 - aLon2) * (bLat2 - aLat1) + (aLat2 - aLat1) * (bLon2 - aLon1)) / det;
            return (0 < lambda && lambda < 1) && (0 < gamma && gamma < 1);
        }
    }

    public static boolean lineCrossesCircle(
            double lat1, double lon1, double lat2, double lon2, double circleLat, double circleLon, double circleRad) {
        return (Math.abs((lat2 - lat1) * circleLon + circleLat * (lon1 - lon2)
                + (lat1 - lat2) * lon1
                + (lon1 - lon2) * lat1) / Math.sqrt(Math.pow((lat2 - lat1), 2)
                + Math.pow((lon1 - lon2), 2)) <= circleRad);
    }

}
