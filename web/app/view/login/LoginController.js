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

Ext.define('Traccar.view.login.LoginController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.login',
    
    requires: [
        'Traccar.view.login.Register'
    ],

    init: function() {
        this.lookupReference('registerButton').setDisabled(
            !Traccar.getApplication().getServer().get('registration')
        );
    },

    onAfterRender: function(field) {
        field.focus();
    },

    onSpecialKey: function(field, e) {
        if (e.getKey() === e.ENTER) {
            this.doLogin();
        }
    },
    
    onLoginClick: function() {
        this.doLogin();
    },
    
    doLogin: function() {
        var form = this.lookupReference('form');
        if (form.isValid()) {
            Ext.getBody().mask(strings.shared_loading);
            
            Traccar.LoginManager.login({
                data: form.getValues(),
                scope: this,
                callback: 'onLoginReturn'
            });
        }
    },
    
    onLoginReturn: function(success) {
        Ext.getBody().unmask();
        if (success) {
            this.fireViewEvent('login');
        } else {
            Traccar.ErrorManager.error(strings.login_failed);
        }
    },
    
    onRegisterClick: function() {
        Ext.create('Traccar.view.login.Register').show();
    }

});
