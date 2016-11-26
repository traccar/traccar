/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

public class GeofencePolygon extends GeofenceGeometry {

    public GeofencePolygon() {
    }

    public GeofencePolygon(String wkt) throws ParseException {
        fromWkt(wkt);
    }

    private ArrayList<Coordinate> coordinates;

    private double[] constant;
    private double[] multiple;

    private void precalc() {
        if (coordinates == null) {
            return;
        }
        int polyCorners = coordinates.size();
        int i;
        int j = polyCorners - 1;

        if (constant != null) {
            constant = null;
        }
        if (multiple != null) {
            multiple = null;
        }

        constant = new double[polyCorners];
        multiple = new double[polyCorners];


        for (i = 0; i < polyCorners; j = i++) {
            if (coordinates.get(j).getLon360() == coordinates.get(i).getLon360()) {
                constant[i] = coordinates.get(i).getLat();
                multiple[i] = 0;
            } else {
                constant[i] = coordinates.get(i).getLat()
                        - (coordinates.get(i).getLon360() * coordinates.get(j).getLat())
                        / (coordinates.get(j).getLon360() - coordinates.get(i).getLon360())
                        + (coordinates.get(i).getLon360() * coordinates.get(i).getLat())
                        / (coordinates.get(j).getLon360() - coordinates.get(i).getLon360());
                multiple[i] = (coordinates.get(j).getLat() - coordinates.get(i).getLat())
                        / (coordinates.get(j).getLon360() - coordinates.get(i).getLon360());
            }
        }
    }

    @Override
    public boolean containsPoint(double latitude, double longitude) {

        int polyCorners = coordinates.size();
        int i;
        int j = polyCorners - 1;
        double longitude360 = longitude + Coordinate.DEGREE360;
        boolean oddNodes = false;

        for (i = 0; i < polyCorners; j = i++) {
            if (coordinates.get(i).getLon360() < longitude360
                    && coordinates.get(j).getLon360() >= longitude360
                    || coordinates.get(j).getLon360() < longitude360
                    && coordinates.get(i).getLon360() >= longitude360) {
                oddNodes ^= longitude360 * multiple[i] + constant[i] < latitude;
            }
        }
        return oddNodes;
    }

    @Override
    public String toWkt() {
        StringBuilder buf = new StringBuilder();
        buf.append("POLYGON ((");
        for (Coordinate coordinate : coordinates) {
            buf.append(String.valueOf(coordinate.getLat()));
            buf.append(" ");
            buf.append(String.valueOf(coordinate.getLon()));
            buf.append(", ");
        }
        return buf.substring(0, buf.length() - 2) + "))";
    }

    @Override
    public void fromWkt(String wkt) throws ParseException {
        if (coordinates == null) {
            coordinates = new ArrayList<>();
        } else {
            coordinates.clear();
        }

        if (!wkt.startsWith("POLYGON")) {
            throw new ParseException("Mismatch geometry type", 0);
        }
        String content = wkt.substring(wkt.indexOf("((") + 2, wkt.indexOf("))"));
        if (content.isEmpty()) {
            throw new ParseException("No content", 0);
        }
        String[] commaTokens = content.split(",");
        if (commaTokens.length < 3) {
            throw new ParseException("Not valid content", 0);
        }

        for (String commaToken : commaTokens) {
            String[] tokens = commaToken.trim().split("\\s");
            if (tokens.length != 2) {
                throw new ParseException("Here must be two coordinates: " + commaToken, 0);
            }
            Coordinate coordinate = new Coordinate();
            try {
                coordinate.setLat(Double.parseDouble(tokens[0]));
            } catch (NumberFormatException e) {
                throw new ParseException(tokens[0] + " is not a double", 0);
            }
            try {
                coordinate.setLon(Double.parseDouble(tokens[1]));
            } catch (NumberFormatException e) {
                throw new ParseException(tokens[1] + " is not a double", 0);
            }
            coordinates.add(coordinate);
        }
        precalc();
    }

}
