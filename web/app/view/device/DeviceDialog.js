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

Ext.define('Traccar.view.device.DeviceDialog', {
    extend: 'Ext.window.Window',
    xtype: 'device-dialog',

    requires: [
        'Traccar.view.device.DeviceDialogController'
    ],

    controller: 'devicedialog',
    
    bodyPadding: styles.panel_padding,
    title: strings.device_dialog,
    resizable: false,
    modal: true,
    
    items: {
        xtype: 'form',
        items: [{
            xtype: 'textfield',
            name: 'name',
            fieldLabel: strings.device_name,
            allowBlank: false
        }, {
            xtype: 'textfield',
            name: 'uniqueId',
            fieldLabel: strings.device_identifier,
            allowBlank: false
        }]
    },

    buttons: [{
        text: strings.dialog_save,
        handler: 'onSaveClick'
    }, {
        text: strings.dialog_cancel,
        handler: 'onCancelClick'
    }]

});
