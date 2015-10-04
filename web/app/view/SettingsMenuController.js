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

Ext.define('Traccar.view.SettingsMenuController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.settings',

    requires: [
        'Traccar.view.LoginController'
    ],

    init: function () {
        if (Traccar.app.getUser().get('admin')) {
            this.lookupReference('settingsServerButton').setHidden(false);
            this.lookupReference('settingsUsersButton').setHidden(false);
        }
    },

    onUserClick: function () {
        var dialog = Ext.create('Traccar.view.UserDialog');
        dialog.down('form').loadRecord(Traccar.app.getUser());
        dialog.show();
    },

    onServerClick: function () {
        var dialog = Ext.create('Traccar.view.ServerDialog');
        dialog.down('form').loadRecord(Traccar.app.getServer());
        dialog.show();
    },

    onUsersClick: function () {
        Ext.create('Ext.window.Window', {
            title: Strings.settingsUsers,
            width: Traccar.Style.windowWidth,
            height: Traccar.Style.windowHeight,
            layout: 'fit',
            modal: true,
            items: {
                xtype: 'userView'
            }
        }).show();
    },

    onLogoutClick: function () {
        Ext.create('Traccar.view.LoginController').logout();
    }
});
