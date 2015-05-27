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

Ext.define('Traccar.view.map.MapController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.map',
    
    config: {
        listen: {
            controller: {
                '*': {
                    reportShow: 'reportShow',
                    reportClear: 'reportClear'
                }
            }
        }
    },

    getLineStyle: function(color) {
        return new ol.style.Style({
            fill: new ol.style.Fill({
                color: color
            }),
            stroke: new ol.style.Stroke({
                color: color,
                width: styles.map_route_width
            })
        });
    },

    getMarkerStyle: function(color) {
        return new ol.style.Style({
            text: new ol.style.Text({
                text: '\uf041',
                font: 'normal 32px FontAwesome',
                textBaseline: 'Bottom',
                fill: new ol.style.Fill({
                    color: color
                }),
                stroke: new ol.style.Stroke({
                    color: 'black',
                    width: 2
                })
            })
        });
    },
    
    reportShow: function() {
        var vectorSource = this.getView().vectorSource;

        var data = Ext.getStore('Positions').getData().clone();
        data.sort('fixTime');
        
        var index;
        var positions = [];
        for (index = 0; index < data.getCount(); index++) {
            positions.push(ol.proj.fromLonLat([
                data.getAt(index).data.longitude,
                data.getAt(index).data.latitude
            ]));
        }

        this.reportRoute = new ol.Feature({
            geometry: new ol.geom.LineString(positions)
        });
        
        /*this.reportStart = new ol.Feature({
            geometry: new ol.geom.Circle(positions[0], styles.map_marker_radius)
        });
        
        this.reportFinish = new ol.Feature({
            geometry: new ol.geom.Circle(positions[positions.length - 1], styles.map_marker_radius)
        });*/

        this.reportRoute.setStyle(this.getLineStyle(styles.map_report_route));
        //this.reportStart.setStyle(this.getLineStyle(styles.map_report_marker));
        //this.reportFinish.setStyle(this.getLineStyle(styles.map_report_marker));

        vectorSource.addFeature(this.reportRoute);
        //vectorSource.addFeature(this.reportStart);
        //vectorSource.addFeature(this.reportFinish);
    },

    reportClear: function() {
        var vectorSource = this.getView().vectorSource;
        
        vectorSource.removeFeature(this.reportRoute);
        //vectorSource.removeFeature(this.reportStart);
        //vectorSource.addFeature(this.reportFinish);
    }

});
