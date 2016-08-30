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
        'Traccar.view.LoginController',
        'Traccar.view.UserDialog',
        'Traccar.view.ServerDialog',
        'Traccar.view.Users',
        'Traccar.view.Groups',
        'Traccar.view.Geofences',
        'Traccar.view.Notifications',
        'Traccar.view.BaseWindow'
    ],

    init: function () {
        var admin, readonly;
        admin = Traccar.app.getUser().get('admin');
        readonly = Traccar.app.getServer().get('readonly');
        if (admin) {
            this.lookupReference('settingsServerButton').setHidden(false);
            this.lookupReference('settingsUsersButton').setHidden(false);
        }
        if (admin || !readonly) {
            this.lookupReference('settingsGroupsButton').setHidden(false);
            this.lookupReference('settingsGeofencesButton').setHidden(false);
            this.lookupReference('settingsNotificationsButton').setHidden(false);
        }
    },

    onUserClick: function () {
        var dialog = Ext.create('Traccar.view.UserDialog');
        dialog.down('form').loadRecord(Traccar.app.getUser());
        dialog.show();
    },

    onGroupsClick: function () {
        Ext.create('Traccar.view.BaseWindow', {
            title: Strings.settingsGroups,
            modal: false,
            items: {
                xtype: 'groupsView'
            }
        }).show();
    },

    onGeofencesClick: function () {
        Ext.create('Traccar.view.BaseWindow', {
            title: Strings.sharedGeofences,
            modal: false,
            items: {
                xtype: 'geofencesView'
            }
        }).show();
    },

    onServerClick: function () {
        var dialog = Ext.create('Traccar.view.ServerDialog');
        dialog.down('form').loadRecord(Traccar.app.getServer());
        dialog.show();
    },

    onUsersClick: function () {
        Ext.create('Traccar.view.BaseWindow', {
            title: Strings.settingsUsers,
            modal: false,
            items: {
                xtype: 'usersView'
            }
        }).show();
    },

    onNotificationsClick: function () {
        var user = Traccar.app.getUser();
        Ext.create('Traccar.view.BaseWindow', {
            title: Strings.sharedNotifications,
            modal: false,
            items: {
                xtype: 'notificationsView',
                user: user
            }
        }).show();
    },

    onLogoutClick: function () {
        Ext.create('Traccar.view.LoginController').logout();
    }
});
