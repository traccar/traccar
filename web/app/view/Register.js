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

Ext.define('Traccar.view.Register', {
    extend: 'Traccar.view.BaseDialog',

    requires: [
        'Traccar.view.RegisterController'
    ],

    controller: 'register',

    items: {
        xtype: 'form',
        reference: 'form',
        jsonSubmit: true,

        items: [{
            xtype: 'textfield',
            name: 'name',
            fieldLabel: Strings.userName,
            allowBlank: false
        }, {
            xtype: 'textfield',
            name: 'email',
            fieldLabel: Strings.userEmail,
            vtype: 'email',
            allowBlank: false
        }, {
            xtype: 'textfield',
            name: 'password',
            fieldLabel: Strings.userPassword,
            inputType: 'password',
            allowBlank: false
        }]
    },

    buttons: [{
        text: Strings.sharedSave,
        handler: 'onCreateClick'
    }, {
        text: Strings.sharedCancel,
        handler: 'closeView'
    }]
});
