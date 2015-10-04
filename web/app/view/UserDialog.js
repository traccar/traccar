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

Ext.define('Traccar.view.UserDialog', {
    extend: 'Traccar.view.BaseEditDialog',

    requires: [
        'Traccar.view.UserDialogController'
    ],

    controller: 'userDialog',

    title: Strings.settingsUser,

    items: {
        xtype: 'form',
        items: [{
            xtype: 'textfield',
            name: 'name',
            fieldLabel: Strings.userName
        }, {
            xtype: 'textfield',
            name: 'email',
            fieldLabel: Strings.userEmail,
            allowBlank: false
        }, {
            xtype: 'textfield',
            name: 'password',
            fieldLabel: Strings.userPassword,
            inputType: 'password',
            allowBlank: false
        }, {
            xtype: 'checkboxfield',
            name: 'admin',
            fieldLabel: Strings.userAdmin,
            allowBlank: false,
            disabled: true,
            reference: 'adminField'
        }, {
            xtype: 'combobox',
            name: 'map',
            fieldLabel: Strings.mapLayer,
            store: 'MapTypes',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'combobox',
            name: 'distanceUnit',
            fieldLabel: Strings.settingsDistanceUnit,
            store: 'DistanceUnits',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'combobox',
            name: 'speedUnit',
            fieldLabel: Strings.settingsSpeedUnit,
            store: 'SpeedUnits',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'numberfield',
            name: 'latitude',
            fieldLabel: Strings.positionLatitude
        }, {
            xtype: 'numberfield',
            name: 'longitude',
            fieldLabel: Strings.positionLongitude
        }, {
            xtype: 'numberfield',
            name: 'zoom',
            fieldLabel: Strings.serverZoom
        }]
    }
});
