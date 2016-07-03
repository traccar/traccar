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
    bodyBorder: true,

    tbar: {
        items: [{
            xtype: 'combobox',
            store: 'GeozoneTypes',
            valueField: 'id',
            displayField: 'name'
        }, {
            xtype: 'tbfill'
        }, {
            text: Strings.sharedSave,
            handler: 'onSaveClick'
        }, {
            text: Strings.sharedCancel,
            handler: 'onCancelClick'
        }]
    },

    initMap: function () {
        var map, featureOverlay;
        this.callParent();

        map = this.map;

        this.features = new ol.Collection();
        featureOverlay = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: this.features
            }),
            style: new ol.style.Style({
                fill: new ol.style.Fill({
                    color: Traccar.Style.mapColorOverlay
                }),
                stroke: new ol.style.Stroke({
                    color: Traccar.Style.mapColorReport,
                    width: Traccar.Style.mapRouteWidth
                }),
                image: new ol.style.Circle({
                    radius: Traccar.Style.mapRadiusNormal,
                    fill: new ol.style.Fill({
                        color: Traccar.Style.mapColorReport
                    })
                })
            })
        });
        featureOverlay.setMap(map);

        map.addInteraction(new ol.interaction.Modify({
            features: this.features,
            deleteCondition: function(event) {
                return ol.events.condition.shiftKeyOnly(event) && ol.events.condition.singleClick(event);
            }
        }));
    },

    addInteraction: function () {
        this.draw = new ol.interaction.Draw({
            features: this.features,
            type: 'Polygon' // (typeSelect.value)
        });
        this.map.addInteraction(this.draw);
    },

    removeInteraction: function () {
        this.map.removeInteraction(this.draw);
    }
});
