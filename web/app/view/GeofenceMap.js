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

Ext.define('Traccar.view.GeofenceMap', {
    extend: 'Traccar.view.BaseMap',
    xtype: 'geofenceMapView',

    requires: [
        'Traccar.view.GeofenceMapController'
    ],

    controller: 'geofenceMap',

    initMap: function () {
        this.callParent();

        var map = this.map;

        var features = new ol.Collection();
        var featureOverlay = new ol.layer.Vector({
            source: new ol.source.Vector({features: features}),
            style: new ol.style.Style({
                fill: new ol.style.Fill({
                    color: 'rgba(255, 255, 255, 0.2)'
                }),
                stroke: new ol.style.Stroke({
                    color: '#ffcc33',
                    width: 2
                }),
                image: new ol.style.Circle({
                    radius: 7,
                    fill: new ol.style.Fill({
                        color: '#ffcc33'
                    })
                })
            })
        });
        featureOverlay.setMap(map);

        var modify = new ol.interaction.Modify({
            features: features,
            // the SHIFT key must be pressed to delete vertices, so
            // that new vertices can be drawn at the same position
            // of existing vertices
            deleteCondition: function(event) {
                return ol.events.condition.shiftKeyOnly(event) &&
                    ol.events.condition.singleClick(event);
            }
        });
        map.addInteraction(modify);

        var draw; // global so we can remove it later
        //var typeSelect = document.getElementById('type');

        function addInteraction() {
            draw = new ol.interaction.Draw({
                features: features,
                type: 'Polygon' // (typeSelect.value)
            });
            map.addInteraction(draw);
        }

        /*typeSelect.onchange = function() {
            map.removeInteraction(draw);
            addInteraction();
        };*/

        addInteraction();
    }
});
