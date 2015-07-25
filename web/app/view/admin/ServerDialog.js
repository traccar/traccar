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

Ext.define('Traccar.view.admin.ServerDialog', {
    extend: 'Ext.window.Window',
    xtype: 'server-dialog',

    requires: [
        'Traccar.view.admin.ServerDialogController'
    ],

    controller: 'serverdialog',
    
    bodyPadding: styles.panel_padding,
    title: strings.server_title,
    resizable: false,
    modal: true,
    
    items: {
        xtype: 'form',
        items: [{
            xtype: 'checkboxfield',
            name: 'registration',
            fieldLabel: strings.server_registration,
            allowBlank: false
        }, {
            xtype: 'combobox',
            name: 'map',
            fieldLabel: strings.map_layer,
            store: 'MapTypes',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'combobox',
            name: 'distanceUnit',
            fieldLabel: strings.settings_distance_unit,
            store: 'DistanceUnits',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'combobox',
            name: 'speedUnit',
            fieldLabel: strings.settings_speed_unit,
            store: 'SpeedUnits',
            displayField: 'name',
            valueField: 'key'
        }, {
            xtype: 'numberfield',
            name: 'latitude',
            fieldLabel: strings.position_latitude
        }, {
            xtype: 'numberfield',
            name: 'longitude',
            fieldLabel: strings.position_longitude
        }, {
            xtype: 'numberfield',
            name: 'zoom',
            fieldLabel: strings.server_zoom
        }]
    },

    buttons: [{
        text: strings.shared_save,
        handler: 'onSaveClick'
    }, {
        text: strings.shared_cancel,
        handler: 'onCancelClick'
    }]

});
