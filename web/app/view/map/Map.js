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
    xtype: 'map-view',

    requires: [
        'Traccar.view.map.MapController'
    ],

    controller: 'map',

    title: strings.map_title,
    layout: 'fit',
    
    /*update: function() {
        Ext.Ajax.request({
            scope: this,
            url: '/api/async',
            success: function(response) {
                var data = Ext.decode(response.responseText).data;
                
                var i;
                for (i = 0; i < data.length; i++) {
                    var iconFeature = new ol.Feature({
                        geometry: new ol.geom.Point([30, 30])
                    });
                    this.vectorSource.addFeature(iconFeature);   
                }
                
                this.update();
            },
            failure: function() {
                // error
            }
        });
    },*/

    listeners: {
        afterrender: function() {

            var transform = this.transform;

            /*var bindKey = 'AseEs0DLJhLlTNoxbNXu7DGsnnH4UoWuGue7-irwKkE3fffaClwc9q_Mr6AyHY8F';

            var layer = new ol.layer.Tile({ source: new ol.source.BingMaps({
                key: bindKey,
                imagerySet: 'Road'
            })});

            var layer = new ol.layer.Tile({ source: new ol.source.BingMaps({
                key: bindKey,
                imagerySet: 'Aerial'
            })});*/

            var layer = new ol.layer.Tile({ source: new ol.source.OSM({
            })});
            
            this.vectorSource = new ol.source.Vector({});
            var vectorLayer = new ol.layer.Vector({
                source: this.vectorSource,
                style: new ol.style.Style({
                    text: new ol.style.Text({
                        text: '\uf041',
                        font: 'normal 32px FontAwesome',
                        textBaseline: 'Bottom',
                        fill: new ol.style.Fill({
                            color: 'red'
                        }),
                        stroke: new ol.style.Stroke({
                            color: 'black',
                            width: 2
                        })
                    })
                })
            });

            var view = new ol.View({
                center: ol.proj.fromLonLat(styles.map_center),
                zoom: styles.map_zoom,
                maxZoom: styles.map_max_zoom
            });

            this.map = new ol.Map({
                target: this.body.dom.id,
                layers: [ layer, vectorLayer ],
                view: view
            });

            var iconFeature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([-1.257778, 51.751944]))
            });

            this.vectorSource.addFeature(iconFeature);

            //this.update();
        },

        resize: function() {
            this.map.updateSize();
        }
    }

});
