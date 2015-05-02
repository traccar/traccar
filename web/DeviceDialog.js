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

Ext.define('DeviceForm', {
    extend: 'Ext.form.Panel',
    xtype: 'device-form',

    defaultType: 'textfield',
    bodyPadding: Styles.panel_padding,

    defaults: { anchor: '100%' },

    url: '/api/device/add',
    jsonSubmit: true,

    items: [{
        allowBlank: false,
        fieldLabel: Strings.device_name,
        name: 'name'
    }, {
        allowBlank: false,
        fieldLabel: Strings.device_identifier,
        name: 'uniqueId'
    }],

    buttons: [{
        text: Strings.dialog_create,
        handler: function() {
            var win = this.up('window');
            var form = this.up('form').getForm();
            if (form.isValid()) {
                form.submit({
                    success: function() {
                        win.close();
                        win.onUpdate();
                    },
                    failure: function() {
                        win.close();
                    }
                });
            }
        }
    }, {
        text: Strings.dialog_cancel,
        handler: function() {
            this.up('window').close();
        }
    }]
});

Ext.define('DeviceDialog', {
    extend: 'Ext.window.Window',
    
    modal: true,

    title: Strings.device_dialog,

    items: [{ xtype: 'device-form' }]
});
