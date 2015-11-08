/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

Ext.define('Traccar.view.MapController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.map',

    config: {
        listen: {
            controller: {
                '*': {
                    selectDevice: 'selectDevice',
                    selectReport: 'selectReport'
                }
            },
            store: {
                '#LatestPositions': {
                    add: 'updateLatest',
                    update: 'updateLatest'
                },
                '#Positions': {
                    load: 'loadReport',
                    clear: 'clearReport'
                }
            },
            component: {
                '#': {
                    selectFeature: 'selectFeature'
                }
            }
        }
    },

    init: function () {
        this.latestMarkers = {};
        this.reportMarkers = {};
    },

    updateLatest: function (store, data) {
        var i, position, geometry, deviceId, name, marker, style;

        if (!Ext.isArray(data)) {
            data = [data];
        }

        for (i = 0; i < data.length; i++) {
            position = data[i];
            deviceId = position.get('deviceId');

            geometry = new ol.geom.Point(ol.proj.fromLonLat([
                position.get('longitude'),
                position.get('latitude')
            ]));

            if (deviceId in this.latestMarkers) {
                marker = this.latestMarkers[deviceId];
                marker.setGeometry(geometry);
            } else {
                marker = new ol.Feature(geometry);
                this.latestMarkers[deviceId] = marker;
                this.getView().getVectorSource().addFeature(marker);

                style = this.getLatestMarker();
                style.getImage().setRotation(position.get('course'));
                style.getText().setText(
                    Ext.getStore('Devices').findRecord('id', deviceId, 0, false, false, true).get('name'));
                marker.setStyle(style);
            }
        }
    },

    loadReport: function (store, data) {
        var i, position, point, points, geometry, deviceId, name, marker, style;

        this.clearReport(store);

        this.reportRoute = new ol.Feature({
            geometry: new ol.geom.LineString([])
        });
        this.reportRoute.setStyle(this.getRouteStyle());
        this.getView().getVectorSource().addFeature(this.reportRoute);

        for (i = 0; i < data.length; i++) {
            position = data[i];

            point = ol.proj.fromLonLat([
                position.get('longitude'),
                position.get('latitude')
            ]);
            geometry = new ol.geom.Point(point);

            marker = new ol.Feature(geometry);
            this.reportMarkers[position.get('id')] = marker;
            this.getView().getVectorSource().addFeature(marker);

            style = this.getReportMarker();
            style.getImage().setRotation(position.get('course'));
            // style.getText().setText('2000-01-01 00:00:00'); // TODO show time
            marker.setStyle(style);

            this.reportRoute.getGeometry().appendCoordinate(point);
        }
    },

    clearReport: function (store) {
        var vectorSource = this.getView().getVectorSource();

        if (this.reportRoute) {
            vectorSource.removeFeature(this.reportRoute);
            this.reportRoute = null;
        }

        if (this.reportMarkers) {
            for (var key in this.reportMarkers) {
                if (this.reportMarkers.hasOwnProperty(key)) {
                    vectorSource.removeFeature(this.reportMarkers[key]);
                }
            }
            this.reportMarkers = {};
        }
    },

    getRouteStyle: function () {
        return new ol.style.Style({
            stroke: new ol.style.Stroke({
                color: Traccar.Style.mapRouteColor,
                width: Traccar.Style.mapRouteWidth
            })
        });
    },

    getMarkerStyle: function (radius, color) {
        return new ol.style.Style({
            image: new ol.style.Arrow({
                radius: radius,
                fill: new ol.style.Fill({
                    color: color
                }),
                stroke: new ol.style.Stroke({
                    color: Traccar.Style.mapArrowStrokeColor,
                    width: Traccar.Style.mapArrowStrokeWidth
                })
            }),
            text: new ol.style.Text({
                textBaseline: 'bottom',
                fill: new ol.style.Fill({
                    color: Traccar.Style.mapTextColor
                }),
                stroke: new ol.style.Stroke({
                    color: Traccar.Style.mapTextStrokeColor,
                    width: Traccar.Style.mapTextStrokeWidth
                }),
                offsetY: -radius / 2 - Traccar.Style.mapTextOffset,
                font : Traccar.Style.mapTextFont
            })
        });
    },

    getLatestMarker: function () {
        return this.getMarkerStyle(
            Traccar.Style.mapRadiusNormal, Traccar.Style.mapColorUnknown);
    },

    getReportMarker: function () {
        return this.getMarkerStyle(
            Traccar.Style.mapRadiusNormal, Traccar.Style.mapColorReport);
    },

    resizeMarker: function (style, radius) {
        return new ol.style.Style({
            image: new ol.style.Arrow({
                radius: radius,
                fill: style.getImage().getFill(),
                stroke: style.getImage().getStroke()
            }),
            text: style.getText()
        });
    },

    selectMarker: function (marker) {
        if (this.selectedMarker) {
            this.selectedMarker.setStyle(
                this.resizeMarker(this.selectedMarker.getStyle(), Traccar.Style.mapRadiusNormal));
        }

        if (marker) {
            marker.setStyle(
                this.resizeMarker(marker.getStyle(), Traccar.Style.mapRadiusSelected));
            this.getView().getMapView().setCenter(marker.getGeometry().getCoordinates());
        }

        this.selectedMarker = marker;
    },

    selectDevice: function (device) {
        this.selectMarker(this.latestMarkers[device.get('id')]);
    },

    selectReport: function (position) {
        this.selectMarker(this.reportMarkers[position.get('id')]);
    },

    selectFeature: function (feature) {
        console.log(feature);
    }
});
