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

Ext.define('Traccar.view.ServerDialog', {
    extend: 'Traccar.view.BaseEditDialog',

    requires: [
        'Traccar.view.BaseEditDialogController'
    ],

    controller: 'baseEditDialog',
    title: strings.serverTitle,

    items: {
        xtype: 'form',
        items: [{
            xtype: 'checkboxfield',
            name: 'registration',
            fieldLabel: strings.serverRegistration,
            allowBlank: false
        }, {
            xtype: 'combobox',
            name: 'map',
            fieldLabel: strings.mapLayer,
            store: 'MapTypes',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'textfield',
            name: 'bingKey',
            fieldLabel: strings.mapBingKey
        }, {
            xtype: 'textfield',
            name: 'mapUrl',
            fieldLabel: strings.mapCustom
        }, {
            xtype: 'combobox',
            name: 'distanceUnit',
            fieldLabel: strings.settingsDistanceUnit,
            store: 'DistanceUnits',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'combobox',
            name: 'speedUnit',
            fieldLabel: strings.settingsSpeedUnit,
            store: 'SpeedUnits',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'numberfield',
            name: 'latitude',
            fieldLabel: strings.positionLatitude
        }, {
            xtype: 'numberfield',
            name: 'longitude',
            fieldLabel: strings.positionLongitude
        }, {
            xtype: 'numberfield',
            name: 'zoom',
            fieldLabel: strings.serverZoom
        }]
    }
});
