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

Ext.define('Traccar.view.Login', {
    extend: 'Traccar.view.BaseDialog',
    alias: 'widget.login',

    requires: [
        'Traccar.view.LoginController'
    ],

    controller: 'login',

    closable: false,
    modal: false,

    items: {
        xtype: 'form',
        reference: 'form',

        autoEl: {
            tag: 'form',
            method: 'POST',
            action: 'blank',
            target: 'submitTarget'
        },

        items: [{
            xtype: 'combobox',
            name: 'language',
            fieldLabel: Strings.loginLanguage,
            store: 'Languages',
            displayField: 'name',
            valueField: 'code',
            submitValue: false,
            listeners: {
                select: 'onSelectLanguage'
            },
            reference: 'languageField'
        }, {
            xtype: 'textfield',
            name: 'email',
            fieldLabel: Strings.userEmail,
            allowBlank: false,
            enableKeyEvents: true,
            listeners: {
                specialKey: 'onSpecialKey',
                afterrender: 'onAfterRender'
            },
            inputAttrTpl: ['autocomplete="on"']
        }, {
            xtype: 'textfield',
            name: 'password',
            fieldLabel: Strings.userPassword,
            inputType: 'password',
            allowBlank: false,
            enableKeyEvents: true,
            listeners: {
                specialKey: 'onSpecialKey'
            },
            inputAttrTpl: ['autocomplete="on"']
        }, {
            xtype: 'component',
            html: '<iframe id="submitTarget" name="submitTarget" style="display:none"></iframe>'
        }, {
            xtype: 'component',
            html: '<input type="submit" id="submitButton" style="display:none">'
        }]
    },

    buttons: [{
        text: Strings.loginRegister,
        handler: 'onRegisterClick',
        reference: 'registerButton'
    }, {
        text: Strings.loginLogin,
        handler: 'onLoginClick'
    }]
});
