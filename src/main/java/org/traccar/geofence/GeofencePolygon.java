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

import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.ShapeFactory;
import org.locationtech.spatial4j.shape.jts.JtsShapeFactory;
import org.traccar.config.Config;
import org.traccar.model.Geofence;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class GeofencePolygon extends GeofenceGeometry {

    private final List<Coordinate> coordinates;

    private final double[] constant;
    private final double[] multiple;

    private final boolean needNormalize;

    public GeofencePolygon(String wkt) throws ParseException {
        coordinates = fromWkt(wkt);

        int polyCorners = coordinates.size();
        int i;
        int j = polyCorners - 1;

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
    public boolean containsPoint(Config config, Geofence geofence, double latitude, double longitude) {

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

    @Override
    public double calculateArea() {
        JtsShapeFactory jtsShapeFactory = new JtsSpatialContextFactory().newSpatialContext().getShapeFactory();
        ShapeFactory.PolygonBuilder polygonBuilder = jtsShapeFactory.polygon();
        for (Coordinate coordinate : coordinates) {
            polygonBuilder.pointXY(coordinate.getLon(), coordinate.getLat());
        }
        return polygonBuilder.build().getArea(SpatialContext.GEO) * DistanceUtils.DEG_TO_KM * DistanceUtils.DEG_TO_KM;
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

    public List<Coordinate> fromWkt(String wkt) throws ParseException {
        List<Coordinate> coordinates = new ArrayList<>();

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

        return coordinates;
    }

}
