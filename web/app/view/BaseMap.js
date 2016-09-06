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

Ext.define('Traccar.view.BaseMap', {
    extend: 'Ext.form.Panel',
    xtype: 'baseMapView',

    layout: 'fit',

    getMap: function () {
        return this.map;
    },

    getMapView: function () {
        return this.mapView;
    },

    initMap: function () {
        var user, server, layer, type, bingKey, lat, lon, zoom, target;

        user = Traccar.app.getUser();
        server = Traccar.app.getServer();

        type = user.get('map') || server.get('map');
        bingKey = server.get('bingKey');

        if (type === 'custom') {
            layer = new ol.layer.Tile({
                source: new ol.source.XYZ({
                    url: server.get('mapUrl'),
                    attributions: [new ol.Attribution({
                        html: ''
                    })]
                })
            });
        } else if (type === 'bingRoad') {
            layer = new ol.layer.Tile({
                source: new ol.source.BingMaps({
                    key: bingKey,
                    imagerySet: 'Road'
                })
            });
        } else if (type === 'bingAerial') {
            layer = new ol.layer.Tile({
                source: new ol.source.BingMaps({
                    key: bingKey,
                    imagerySet: 'Aerial'
                })
            });
        } else {
            layer = new ol.layer.Tile({
                source: new ol.source.OSM({})
            });
        }

        lat = user.get('latitude') || server.get('latitude') || Traccar.Style.mapDefaultLat;
        lon = user.get('longitude') || server.get('longitude') || Traccar.Style.mapDefaultLon;
        zoom = user.get('zoom') || server.get('zoom') || Traccar.Style.mapDefaultZoom;

        this.mapView = new ol.View({
            center: ol.proj.fromLonLat([lon, lat]),
            zoom: zoom,
            maxZoom: Traccar.Style.mapMaxZoom
        });

        this.map = new ol.Map({
            target: this.body.dom.id,
            layers: [layer],
            view: this.mapView
        });

        target = this.map.getTarget();
        if (typeof target === 'string') {
            target = Ext.get(target).dom;
        }

        this.map.on('pointermove', function (e) {
            var hit = this.forEachFeatureAtPixel(e.pixel, function (feature, layer) {
                return true;
            });
            if (hit) {
                target.style.cursor = 'pointer';
            } else {
                target.style.cursor = '';
            }
        });

        this.map.on('click', function (e) {
            this.map.forEachFeatureAtPixel(e.pixel, function (feature, layer) {
                this.fireEvent('selectfeature', feature);
            }, this);
        }, this);
    },

    listeners: {
        afterrender: function () {
            this.initMap();
        },

        resize: function () {
            this.map.updateSize();
        }
    }
});
