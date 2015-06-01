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

Ext.define('Traccar.LoginManager', {
    singleton: true,
    
    setUser: function(data) {
        var reader = Ext.create('Ext.data.reader.Json', {
            model: 'Traccar.model.User'
        });
        Traccar.getApplication().setUser(
                reader.readRecords(data).getRecords()[0]);
    },

    server: function(options) {
        Ext.Ajax.request({
            scope: this,
            url: '/api/server/get',
            callback: this.onServerReturn,
            original: options
        });
    },

    onServerReturn: function(options, success, response) {
        options = options.original;
        if (Traccar.ErrorManager.check(success, response)) {
            var result = Ext.decode(response.responseText);
            if (result.success) {
                var reader = Ext.create('Ext.data.reader.Json', {
                    model: 'Traccar.model.Server'
                });
                Traccar.getApplication().setServer(
                    reader.readRecords(result.data).getRecords()[0]);
            }
            Ext.callback(options.callback, options.scope, [result.success]);
        }
    },

    session: function(options) {
        Ext.Ajax.request({
            scope: this,
            url: '/api/session',
            callback: this.onSessionReturn,
            original: options
        });
    },
    
    onSessionReturn: function(options, success, response) {
        options = options.original;
        if (Traccar.ErrorManager.check(success, response)) {
            var result = Ext.decode(response.responseText);
            if (result.success) {
                this.setUser(result.data);
            }
            Ext.callback(options.callback, options.scope, [result.success]);
        }
    },

    login: function(options) {
        Ext.Ajax.request({
            scope: this,
            url: '/api/login',
            params: options.data,
            callback: this.onLoginReturn,
            original: options
        });
    },
    
    onLoginReturn: function(options, success, response) {
        options = options.original;
        if (Traccar.ErrorManager.check(success, response)) {
            var result = Ext.decode(response.responseText);
            if (result.success) {
                this.setUser(result.data);
            }
            Ext.callback(options.callback, options.scope, [result.success]);
        }
    },
    
    logout: function() {
        Ext.Ajax.request({
            scope: this,
            url: '/api/logout',
            callback: this.onLogoutReturn
        });
    },
    
    onLogoutReturn: function() {
        window.location.reload();
    }

});
