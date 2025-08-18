/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.geofence;

import java.text.DecimalFormat;
import java.text.ParseException;

import org.traccar.helper.DistanceCalculator;

public class GeofenceCircle extends GeofenceGeometry {

    private final double centerLatitude;
    private final double centerLongitude;
    private final double radius;

    public GeofenceCircle(String wkt) throws ParseException {
        DecodedCircle decoded = fromWkt(wkt);
        centerLatitude = decoded.latitude;
        centerLongitude = decoded.longitude;
        radius = decoded.radius;
        setMin(new Coordinate(centerLatitude - radius, centerLongitude - radius));
        setMax(new Coordinate(centerLatitude + radius, centerLongitude + radius));
    }

    @Override
    protected boolean containsPointInternal(double latitude, double longitude) {
        return DistanceCalculator.distance(centerLatitude, centerLongitude, latitude, longitude) <= radius;
    }

    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }

    @Override
    public String toWkt() {
        String wkt;
        wkt = "CIRCLE (";
        wkt += String.valueOf(centerLatitude);
        wkt += " ";
        wkt += String.valueOf(centerLongitude);
        wkt += ", ";
        DecimalFormat format = new DecimalFormat("0.#");
        wkt += format.format(radius);
        wkt += ")";
        return wkt;
    }

    public record DecodedCircle(double latitude, double longitude, double radius) {
    }

    public DecodedCircle fromWkt(String wkt) throws ParseException {
        if (!wkt.startsWith("CIRCLE")) {
            throw new ParseException("Mismatch geometry type", 0);
        }
        String content = wkt.substring(wkt.indexOf("(") + 1, wkt.indexOf(")"));
        if (content.isEmpty()) {
            throw new ParseException("No content", 0);
        }
        String[] commaTokens = content.split(",");
        if (commaTokens.length != 2) {
            throw new ParseException("Not valid content", 0);
        }
        String[] tokens = commaTokens[0].split("\\s");
        if (tokens.length != 2) {
            throw new ParseException("Too much or less coordinates", 0);
        }
        double centerLatitude;
        try {
            centerLatitude = Double.parseDouble(tokens[0]);
        } catch (NumberFormatException e) {
            throw new ParseException(tokens[0] + " is not a double", 0);
        }
        double centerLongitude;
        try {
            centerLongitude = Double.parseDouble(tokens[1]);
        } catch (NumberFormatException e) {
            throw new ParseException(tokens[1] + " is not a double", 0);
        }
        double radius;
        try {
            radius = Double.parseDouble(commaTokens[1]);
        } catch (NumberFormatException e) {
            throw new ParseException(commaTokens[1] + " is not a double", 0);
        }
        return new DecodedCircle(centerLatitude, centerLongitude, radius);
    }

}
