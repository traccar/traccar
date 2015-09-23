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

Ext.define('Traccar.controller.Root', {
    extend: 'Ext.app.Controller',
    
    requires: [
        'Traccar.LoginManager'
    ],

    init: function() {
        var indicator = document.createElement('div');
        indicator.className = 'state-indicator';
        document.body.appendChild(indicator);
        this.isPhone = parseInt(window.getComputedStyle(indicator).getPropertyValue('z-index'), 10) !== 0;
    },
    
    onLaunch: function () {
        Traccar.LoginManager.server({
            scope: this,
            callback: 'onServer'
        });
    },

    onServer: function() {
        Traccar.LoginManager.session({
            scope: this,
            callback: 'onSession'
        });
    },
    
    onSession: function(success) {
        if (success) {
            this.loadApp();
        } else {
            this.login = Ext.create('Traccar.view.login.Login', {
                listeners: {
                    scope: this,
                    login: 'onLogin'
                }
            });
            this.login.show();
        }
    },

    onLogin: function() {
        this.login.close();
        this.loadApp();
    },
    
    loadApp: function() {
        Ext.getStore('Devices').load();
        Ext.getBody().empty();
        if (this.isPhone) {
            Ext.create('Traccar.view.MainMobile');
        } else {
            Ext.create('Traccar.view.Main');
        }
    }
});
