/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

Ext.define('Traccar.GeofenceConverter', {
    singleton: true,

    wktToGeometry: function (mapView, wkt) {
        var geometry, projection, resolutionAtEquator, pointResolution, resolutionFactor, points = [], center, radius,
                content, i, lat, lon, coordinates;
        if (wkt.startsWith('POLYGON')) {
            content = wkt.match(/\([^\(\)]+\)/);
            if (content !== null) {
                coordinates = content[0].match(/-?\d+\.?\d*/g);
                if (coordinates !== null) {
                    projection = mapView.getProjection();
                    for (i = 0; i < coordinates.length; i += 2) {
                        lat = Number(coordinates[i]);
                        lon = Number(coordinates[i + 1]);
                        points.push(ol.proj.transform([lon, lat], 'EPSG:4326', projection));
                    }
                    geometry = new ol.geom.Polygon([points]);
                }
            }
        } else if (wkt.startsWith('CIRCLE')) {
            content = wkt.match(/\([^\(\)]+\)/);
            if (content !== null) {
                coordinates = content[0].match(/-?\d+\.?\d*/g);
                if (coordinates !== null) {
                    projection = mapView.getProjection();
                    center = ol.proj.transform([Number(coordinates[1]), Number(coordinates[0])], 'EPSG:4326', projection);
                    resolutionAtEquator = mapView.getResolution();
                    pointResolution = projection.getPointResolution(resolutionAtEquator, center);
                    resolutionFactor = resolutionAtEquator / pointResolution;
                    radius = (Number(coordinates[2]) / ol.proj.METERS_PER_UNIT.m) * resolutionFactor;
                    geometry = new ol.geom.Circle(center, radius);
                }
            }
        }
        return geometry;
    },

    geometryToWkt: function (projection, geometry) {
        var result, i, center, radius, edgeCoordinate, earthSphere, groundRadius, points;
        if (geometry instanceof ol.geom.Circle) {
            center = geometry.getCenter();
            radius = geometry.getRadius();
            edgeCoordinate = [center[0] + radius, center[1]];
            center = ol.proj.transform(center, projection, 'EPSG:4326');
            earthSphere = new ol.Sphere(6378137);
            groundRadius = earthSphere.haversineDistance(center,
                ol.proj.transform(edgeCoordinate, projection, 'EPSG:4326'));
            result = 'CIRCLE (';
            result += center[1] + ' ' + center[0] + ', ';
            result += Number((groundRadius).toFixed(1)) + ')';
        } else if (geometry instanceof ol.geom.Polygon) {
            geometry.transform(projection, 'EPSG:4326');
            points = geometry.getCoordinates();
            result = 'POLYGON((';
            for (i = 0; i < points[0].length; i += 1) {
                result += points[0][i][1] + ' ' + points[0][i][0] + ', ';
            }
            result = result.substring(0, result.length - 2) + '))';
        }
        return result;
    }
});
