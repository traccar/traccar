/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

public class DistanceCalculator {

    private static final double equatorialEarthRadius = 6378.1370D;
    private static final double deg2rad = (Math.PI / 180);

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dlong = (lon2 - lon1) * deg2rad;
        double dlat = (lat2 - lat1) * deg2rad;
        double a = Math.pow(Math.sin(dlat / 2), 2) +
                Math.cos(lat1 * deg2rad) * Math.cos(lat2 * deg2rad) * Math.pow(Math.sin(dlong / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = equatorialEarthRadius * c;
        return d * 1000;
    }

}
