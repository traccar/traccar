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

Ext.define('LoginForm', {
    extend: 'Ext.form.Panel',
    xtype: 'login-form',

    defaultType: 'textfield',
    bodyPadding: Styles.panel_padding,

    defaults: { anchor: '100%' },

    url: '/api/login',

    items: [{
        allowBlank: false,
        fieldLabel: Strings.login_email,
        name: 'email'
    }, {
        allowBlank: false,
        fieldLabel: Strings.login_password,
        name: 'password',
        inputType: 'password'
    }],

    buttons: [{
        text: Strings.login_register
    }, {
        text: Strings.login_login,
        handler: function() {
            var win = this.up('window');
            var form = this.up('form').getForm();
            if (form.isValid()) {
                form.submit({
                    success: function(form, action) {
                        win.close();
                        Ext.create('MainView', { renderTo: document.body });
                    },
                    failure: function(form, action) {
                        Ext.Msg.alert(Strings.login_title, Strings.login_failed);
                    }
                });
            }
        }
    }]
});

Ext.define('Login', {
    extend: 'Ext.window.Window',
    requires: [ 'MainView' ],

    title: Strings.login_title,
    closable: false,

    items: [{ xtype: 'login-form' }]
});
