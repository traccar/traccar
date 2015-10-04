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
        'Traccar.view.Login',
        'Traccar.view.Main',
        'Traccar.view.MainMobile'
    ],

    init: function () {
        var indicator = document.createElement('div');
        indicator.className = 'state-indicator';
        document.body.appendChild(indicator);
        this.isPhone = parseInt(window.getComputedStyle(indicator).getPropertyValue('z-index'), 10) !== 0;
    },

    onLaunch: function () {
        Ext.Ajax.request({
            scope: this,
            url: '/api/server/get',
            callback: this.onServerReturn
        });
    },

    onServerReturn: function (options, success, response) {
        var result;
        if (Traccar.ErrorManager.check(success, response)) {
            result = Ext.decode(response.responseText);
            if (result.success) {
                Traccar.app.setServer(result.data);
                Ext.Ajax.request({
                    scope: this,
                    url: '/api/session',
                    callback: this.onSessionReturn
                });
            }
        }
    },

    onSessionReturn: function (options, success, response) {
        var result;
        if (Traccar.ErrorManager.check(success, response)) {
            result = Ext.decode(response.responseText);
            if (result.success) {
                Traccar.app.setUser(result.data);
                this.loadApp();
            } else {
                this.login = Ext.create('widget.login', {
                    listeners: {
                        scope: this,
                        login: this.onLogin
                    }
                });
                this.login.show();
            }
        }
    },

    onLogin: function () {
        this.login.close();
        this.loadApp();
    },

    loadApp: function () {
        Ext.getStore('Devices').load();
        Ext.getBody().empty();
        if (this.isPhone) {
            Ext.create('widget.mainMobile');
        } else {
            Ext.create('widget.main');
        }
    }
});
