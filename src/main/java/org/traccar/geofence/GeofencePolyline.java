/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.geofence;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.traccar.helper.DistanceCalculator;

public class GeofencePolyline extends GeofenceGeometry {

    private final List<Coordinate> coordinates;
    private final double polylineDistance;

    public GeofencePolyline(String wkt, double polylineDistance) throws ParseException {
        coordinates = fromWkt(wkt);
        calculateBoundary(coordinates, polylineDistance);
        this.polylineDistance = polylineDistance;
    }

    @Override
    protected boolean containsPointInternal(double latitude, double longitude) {
        for (int i = 1; i < coordinates.size(); i++) {
            if (DistanceCalculator.distanceToLine(
                    latitude, longitude, coordinates.get(i - 1).lat(), coordinates.get(i - 1).lon(),
                    coordinates.get(i).lat(), coordinates.get(i).lon()) <= polylineDistance) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double calculateArea() {
        return 0;
    }

    @Override
    public String toWkt() {
        StringBuilder buf = new StringBuilder();
        buf.append("LINESTRING (");
        for (Coordinate coordinate : coordinates) {
            buf.append(coordinate.lat());
            buf.append(" ");
            buf.append(coordinate.lon());
            buf.append(", ");
        }
        return buf.substring(0, buf.length() - 2) + ")";
    }

    private List<Coordinate> fromWkt(String wkt) throws ParseException {
        List<Coordinate> coordinates = new ArrayList<>();

        if (!wkt.startsWith("LINESTRING")) {
            throw new ParseException("Mismatch geometry type", 0);
        }
        String content = wkt.substring(wkt.indexOf("(") + 1, wkt.indexOf(")"));
        if (content.isEmpty()) {
            throw new ParseException("No content", 0);
        }
        String[] commaTokens = content.split(",");
        if (commaTokens.length < 2) {
            throw new ParseException("Not valid content", 0);
        }

        for (String commaToken : commaTokens) {
            String[] tokens = commaToken.trim().split("\\s");
            if (tokens.length != 2) {
                throw new ParseException("Here must be two coordinates: " + commaToken, 0);
            }
            double lat;
            try {
                lat = Double.parseDouble(tokens[0]);
            } catch (NumberFormatException e) {
                throw new ParseException(tokens[0] + " is not a double", 0);
            }
            double lon;
            try {
                lon = Double.parseDouble(tokens[1]);
            } catch (NumberFormatException e) {
                throw new ParseException(tokens[1] + " is not a double", 0);
            }
            coordinates.add(new Coordinate(lat, lon));
        }

        return coordinates;
    }

}
