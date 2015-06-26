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

Ext.define('Traccar.view.device.DeviceController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.device',
    
    requires: [
        'Traccar.view.device.DeviceDialog',
        'Traccar.view.user.UserDialog',
        'Traccar.view.admin.ServerDialog',
        'Traccar.view.user.User'
    ],

    config: {
        listen: {
            controller: {
                '*': {
                    selectReport: 'selectReport'
                }
            }
        }
    },

    init: function() {
        if (Traccar.getApplication().getUser().get('admin')) {
            this.lookupReference('settingsServerButton').setDisabled(false);
            this.lookupReference('settingsUsersButton').setDisabled(false);
        }
    },
    
    onLogoutClick: function() {
        Traccar.LoginManager.logout();
    },
    
    onAddClick: function() {
        var device = Ext.create('Traccar.model.Device');
        var dialog = Ext.create('Traccar.view.device.DeviceDialog');
        dialog.down('form').loadRecord(device);
        dialog.show();
    },
    
    onEditClick: function() {
        var device = this.getView().getSelectionModel().getSelection()[0];
        var dialog = Ext.create('Traccar.view.device.DeviceDialog');
        dialog.down('form').loadRecord(device);
        dialog.show();
    },
    
    onRemoveClick: function() {
        var device = this.getView().getSelectionModel().getSelection()[0];
        Ext.Msg.show({
            title: strings.device_dialog,
            message: strings.device_remove_confirm,
            buttons: Ext.Msg.YESNO,
            buttonText: {
                yes: strings.dialog_delete,
                no: strings.dialog_cancel
            },
            fn: function(btn) {
                if (btn === 'yes') {
                    var store = Ext.getStore('Devices');
                    store.remove(device);
                    store.sync();
                }
            }
        });
    },
    
    onSelectionChange: function(selected) {
        var empty = selected.getCount() === 0;
        this.lookupReference('deviceEditButton').setDisabled(empty);
        this.lookupReference('deviceRemoveButton').setDisabled(empty);
        if (!empty) {
            this.fireEvent("selectDevice", selected.getLastSelected());
        }
    },

    onUserClick: function() {
        var user = Traccar.getApplication().getUser();
        var dialog = Ext.create('Traccar.view.user.UserDialog');
        dialog.down('form').loadRecord(user);
        dialog.show();
    },

    onServerClick: function() {
        var server = Traccar.getApplication().getServer();
        var dialog = Ext.create('Traccar.view.admin.ServerDialog');
        dialog.down('form').loadRecord(server);
        dialog.show();
    },

    onUsersClick: function() {
        Ext.create('Ext.window.Window', {
            title: strings.login_users_title,
            width: styles.window_width,
            height: styles.window_height,
            layout: 'fit',
            modal: true,
            items: {
                xtype: 'user-view'
            }
        }).show();
    },

    selectReport: function(position) {
        if (position !== undefined) {
            this.getView().getSelectionModel().deselectAll();
        }
    }

});
