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

Ext.define('Traccar.view.LoginController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.login',

    requires: [
        'Traccar.view.Register'
    ],

    init: function () {
        this.lookupReference('registerButton').setDisabled(
            !Traccar.app.getServer().get('registration'));
        this.lookupReference('languageField').setValue(Locale.language);
    },

    login: function () {
        var form = this.lookupReference('form');
        if (form.isValid()) {
            Ext.getBody().mask(Strings.sharedLoading);
            Ext.Ajax.request({
                scope: this,
                method: 'POST',
                url: '/api/rest/session',
                params: form.getValues(),
                callback: function (options, success, response) {
                    Ext.getBody().unmask();
                    if (success) {
                        Traccar.app.setUser(Ext.decode(response.responseText));
                        this.fireViewEvent('login');
                    } else {
                        Traccar.app.showError(Strings.loginFailed);
                    }
                }
            });
        }
    },

    logout: function () {
        Ext.Ajax.request({
            scope: this,
            method: 'DELETE',
            url: '/api/rest/session',
            callback: function () {
                window.location.reload();
            }
        });
    },

    onSelectLanguage: function (selected) {
        var paramName, paramValue, url, prefix, suffix;
        paramName = 'locale';
        paramValue = selected.getValue();
        url = window.location.href;
        if (url.indexOf(paramName + '=') >= 0) {
            prefix = url.substring(0, url.indexOf(paramName));
            suffix = url.substring(url.indexOf(paramName));
            suffix = suffix.substring(suffix.indexOf('=') + 1);
            suffix = (suffix.indexOf('&') >= 0) ? suffix.substring(suffix.indexOf('&')) : '';
            url = prefix + paramName + '=' + paramValue + suffix;
        } else {
            if (url.indexOf('?') < 0) {
                url += '?' + paramName + '=' + paramValue;
            } else {
                url += '&' + paramName + '=' + paramValue;
            }
        }
        window.location.href = url;
    },

    onAfterRender: function (field) {
        field.focus();
    },

    onSpecialKey: function (field, e) {
        if (e.getKey() === e.ENTER) {
            this.login();
        }
    },

    onLoginClick: function () {
        Ext.getElementById('submitButton').click();
        this.login();
    },

    onRegisterClick: function () {
        Ext.create('Traccar.view.Register').show();
    }
});
