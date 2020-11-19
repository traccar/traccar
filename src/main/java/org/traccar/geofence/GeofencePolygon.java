/*
 * Copyright 2016 - 2020 Anton Tananaev (anton@traccar.org)
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

    private boolean needNormalize = false;

    private void preCalculate() {
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

        boolean hasNegative = false;
        boolean hasPositive = false;
        for (i = 0; i < polyCorners; i++) {
            if (coordinates.get(i).getLon() > 90) {
                hasPositive = true;
            } else if (coordinates.get(i).getLon() < -90) {
                hasNegative = true;
            }
        }
        needNormalize = hasPositive && hasNegative;

        for (i = 0; i < polyCorners; j = i++) {
            if (normalizeLon(coordinates.get(j).getLon()) == normalizeLon(coordinates.get(i).getLon())) {
                constant[i] = coordinates.get(i).getLat();
                multiple[i] = 0;
            } else {
                constant[i] = coordinates.get(i).getLat()
                        - (normalizeLon(coordinates.get(i).getLon()) * coordinates.get(j).getLat())
                        / (normalizeLon(coordinates.get(j).getLon()) - normalizeLon(coordinates.get(i).getLon()))
                        + (normalizeLon(coordinates.get(i).getLon()) * coordinates.get(i).getLat())
                        / (normalizeLon(coordinates.get(j).getLon()) - normalizeLon(coordinates.get(i).getLon()));
                multiple[i] = (coordinates.get(j).getLat() - coordinates.get(i).getLat())
                        / (normalizeLon(coordinates.get(j).getLon()) - normalizeLon(coordinates.get(i).getLon()));
            }
        }
    }

    private double normalizeLon(double lon) {
        if (needNormalize && lon < -90) {
            return lon + 360;
        }
        return lon;
    }

    @Override
    public boolean containsPoint(double latitude, double longitude) {

        int polyCorners = coordinates.size();
        int i;
        int j = polyCorners - 1;
        double longitudeNorm = normalizeLon(longitude);
        boolean oddNodes = false;

        for (i = 0; i < polyCorners; j = i++) {
            if (normalizeLon(coordinates.get(i).getLon()) < longitudeNorm
                    && normalizeLon(coordinates.get(j).getLon()) >= longitudeNorm
                    || normalizeLon(coordinates.get(j).getLon()) < longitudeNorm
                    && normalizeLon(coordinates.get(i).getLon()) >= longitudeNorm) {
                oddNodes ^= longitudeNorm * multiple[i] + constant[i] < latitude;
            }
        }
        return oddNodes;
    }

    private double toRadians(double input) {
        return input / 180.0 * Math.PI;
    }

    private double polarTriangleArea(double tan1, double lng1, double tan2, double lng2) {
        double deltaLng = lng1 - lng2;
        double t = tan1 * tan2;
        return 2 * Math.atan2(t * Math.sin(deltaLng), 1 + t * Math.sin(deltaLng));
    }

    @Override
    public double calculateArea() {
        if (coordinates.size() < 3) {
            return 0;
        }

        double total = 0;
        Coordinate prev = coordinates.get(coordinates.size() - 1);
        double prevTanLat = Math.tan((Math.PI / 2 - toRadians(prev.getLat())) / 2);
        double prevLng = toRadians(prev.getLon());

        for (Coordinate point : coordinates) {
            double tanLat = Math.tan((Math.PI / 2 - toRadians(point.getLat())) / 2);
            double lng = toRadians(point.getLon());
            total += polarTriangleArea(tanLat, lng, prevTanLat, prevLng);
            prevTanLat = tanLat;
            prevLng = lng;
        }

        double earthRadius = 6371009;
        return total * (earthRadius * earthRadius);
    }

    @Override
    public String toWkt() {
        StringBuilder buf = new StringBuilder();
        buf.append("POLYGON ((");
        for (Coordinate coordinate : coordinates) {
            buf.append(coordinate.getLat());
            buf.append(" ");
            buf.append(coordinate.getLon());
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

        preCalculate();
    }

}
