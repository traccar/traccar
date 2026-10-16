/*
 * Copyright 2016 - 2026 Anton Tananaev (anton@traccar.org)
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

import org.traccar.helper.DistanceCalculator;

import java.util.List;

public abstract class GeofenceGeometry {

    private Coordinate min;
    private Coordinate max;

    protected void setMin(Coordinate min) {
        this.min = min;
    }

    protected void setMax(Coordinate max) {
        this.max = max;
    }

    protected void calculateBoundary(List<Coordinate> coordinates, double padding) {
        var iterator = coordinates.iterator();
        Coordinate current = iterator.next();
        double minLat = current.lat();
        double minLon = current.lon();
        double maxLat = current.lat();
        double maxLon = current.lon();
        while (iterator.hasNext()) {
            current = iterator.next();
            minLat = Math.min(minLat, current.lat());
            minLon = Math.min(minLon, current.lon());
            maxLat = Math.max(maxLat, current.lat());
            maxLon = Math.max(maxLon, current.lon());
        }
        if (padding > 0) {
            double latPadding = DistanceCalculator.getLatitudeDelta(padding);
            double lonPadding = Math.max(
                    DistanceCalculator.getLongitudeDelta(padding, minLat),
                    DistanceCalculator.getLongitudeDelta(padding, maxLat));
            setMin(new Coordinate(minLat - latPadding, minLon - lonPadding));
            setMax(new Coordinate(maxLat + latPadding, maxLon + lonPadding));
        } else {
            setMin(new Coordinate(minLat, minLon));
            setMax(new Coordinate(maxLat, maxLon));
        }
    }

    private boolean intersectsBoundary(
            double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
        if (min.lon >= 0 || max.lon < 0) {
            if (maxLatitude < min.lat || minLatitude > max.lat) {
                return false;
            }
            if (maxLongitude < min.lon || minLongitude > max.lon) {
                return false;
            }
        }
        return true;
    }

    public boolean containsPoint(double latitude, double longitude) {
        if (!intersectsBoundary(latitude, latitude, longitude, longitude)) {
            return false;
        }
        return containsPointInternal(latitude, longitude);
    }

    protected abstract boolean containsPointInternal(double latitude, double longitude);

    public boolean intersectsSegment(double latitude1, double longitude1, double latitude2, double longitude2) {
        if (!intersectsBoundary(
                Math.min(latitude1, latitude2), Math.max(latitude1, latitude2),
                Math.min(longitude1, longitude2), Math.max(longitude1, longitude2))) {
            return false;
        }
        return intersectsSegmentInternal(latitude1, longitude1, latitude2, longitude2);
    }

    protected abstract boolean intersectsSegmentInternal(
            double latitude1, double longitude1, double latitude2, double longitude2);

    protected static boolean intersectsEdges(
            List<Coordinate> coordinates, boolean closed,
            double latitude1, double longitude1, double latitude2, double longitude2) {
        for (int i = closed ? 0 : 1, j = closed ? coordinates.size() - 1 : 0; i < coordinates.size(); j = i++) {
            if (DistanceCalculator.segmentsIntersect(
                    latitude1, longitude1, latitude2, longitude2,
                    coordinates.get(j).lat(), coordinates.get(j).lon(),
                    coordinates.get(i).lat(), coordinates.get(i).lon())) {
                return true;
            }
        }
        return false;
    }

    public abstract double calculateArea();

    public abstract String toWkt();

    public record Coordinate(double lat, double lon) {}

}
