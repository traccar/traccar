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

Ext.define('Traccar.view.user.UserDialog', {
    extend: 'Ext.window.Window',

    requires: [
        'Traccar.view.user.UserDialogController'
    ],

    controller: 'userDialog',
    
    bodyPadding: styles.panelPadding,
    title: strings.settingsUser,
    resizable: false,
    modal: true,
    
    items: {
        xtype: 'form',
        items: [{
            xtype: 'textfield',
            name: 'name',
            fieldLabel: strings.userName
        }, {
            xtype: 'textfield',
            name: 'email',
            fieldLabel: strings.userEmail,
            allowBlank: false
        }, {
            xtype: 'textfield',
            name: 'password',
            fieldLabel: strings.userPassword,
            inputType: 'password',
            allowBlank: false
        }, {
            xtype: 'checkboxfield',
            name: 'admin',
            fieldLabel: strings.userAdmin,
            allowBlank: false,
            disabled: true,
            reference: 'adminField'
        }, {
            xtype: 'combobox',
            name: 'map',
            fieldLabel: strings.mapLayer,
            store: 'MapTypes',
            displayField: 'name',
            valueField: 'key'
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
    },

    buttons: [{
        text: strings.sharedSave,
        handler: 'onSaveClick'
    }, {
        text: strings.sharedCancel,
        handler: 'onCancelClick'
    }]

});
