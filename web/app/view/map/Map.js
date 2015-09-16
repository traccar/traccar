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

Ext.define('Traccar.view.map.Map', {
    extend: 'Ext.form.Panel',
    xtype: 'mapView',

    requires: [
        'Traccar.view.map.MapController'
    ],

    controller: 'map',

    title: strings.mapTitle,
    layout: 'fit',

    listeners: {
        afterrender: function() {

            var user = Traccar.app.getUser();
            var server = Traccar.app.getServer();

            var layer;
            var mapLayer = user.get('map') || server.get('map');

            var bingKey = server.get('bingKey') || 'AseEs0DLJhLlTNoxbNXu7DGsnnH4UoWuGue7-irwKkE3fffaClwc9q_Mr6AyHY8F';

            if (mapLayer === 'custom') {
                layer = new ol.layer.Tile({ source: new ol.source.XYZ({
                    url: server.get('mapUrl')
                })});
            } else if (mapLayer === 'bingRoad') {
                layer = new ol.layer.Tile({ source: new ol.source.BingMaps({
                    key: bingKey,
                    imagerySet: 'Road'
                })});
            } else if (mapLayer === 'bingAerial') {
                layer = new ol.layer.Tile({ source: new ol.source.BingMaps({
                    key: bingKey,
                    imagerySet: 'Aerial'
                })});
            } else {
                layer = new ol.layer.Tile({ source: new ol.source.OSM({
                })});
            }

            this.vectorSource = new ol.source.Vector({});
            var vectorLayer = new ol.layer.Vector({
                source: this.vectorSource
            });

            var lat = user.get('latitude') || server.get('latitude') || styles.mapDefaultLat;
            var lon = user.get('longitude') || server.get('longitude') || styles.mapDefaultLon;
            var zoom = user.get('zoom') || server.get('zoom') || styles.mapDefaultZoom;

            this.mapView = new ol.View({
                center: ol.proj.fromLonLat([ lon, lat ]),
                zoom: zoom,
                maxZoom: styles.mapMaxZoom
            });

            this.map = new ol.Map({
                target: this.body.dom.id,
                layers: [ layer, vectorLayer ],
                view: this.mapView
            });
        },

        resize: function() {
            this.map.updateSize();
        }
    }

});
