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
        'Traccar.view.CommandDialog',
        'Traccar.view.user.UserDialog',
        'Traccar.view.user.User',
        'Traccar.view.login.LoginController'
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
        if (Traccar.app.getUser().get('admin')) {
            this.lookupReference('settingsServerButton').setHidden(false);
            this.lookupReference('settingsUsersButton').setHidden(false);
        }
    },
    
    onLogoutClick: function() {
        Ext.create('Traccar.view.login.LoginController').logout();
    },
    
    onAddClick: function() {
        var device, dialog;
        device = Ext.create('Traccar.model.Device');
        device.store = this.getView().getStore();
        dialog = Ext.create('Traccar.view.DeviceDialog');
        dialog.down('form').loadRecord(device);
        dialog.show();
    },
    
    onEditClick: function() {
        var device, dialog;
        device = this.getView().getSelectionModel().getSelection()[0];
        dialog = Ext.create('Traccar.view.DeviceDialog');
        dialog.down('form').loadRecord(device);
        dialog.show();
    },
    
    onRemoveClick: function() {
        var device = this.getView().getSelectionModel().getSelection()[0];
        Ext.Msg.show({
            title: strings.deviceDialog,
            message: strings.sharedRemoveConfirm,
            buttons: Ext.Msg.YESNO,
            buttonText: {
                yes: strings.sharedRemove,
                no: strings.sharedCancel
            },
            fn: function(btn) {
                var store;
                if (btn === 'yes') {
                    store = Ext.getStore('Devices');
                    store.remove(device);
                    store.sync();
                }
            }
        });
    },

    onCommandClick: function() {
        var device, command, dialog;
        device = this.getView().getSelectionModel().getSelection()[0];
        command = Ext.create('Traccar.model.Command');
        command.set('deviceId', device.get('id'));
        dialog = Ext.create('Traccar.view.CommandDialog');
        dialog.down('form').loadRecord(command);
        dialog.show();
    },

    onSelectionChange: function(selected) {
        var empty = selected.getCount() === 0;
        this.lookupReference('deviceEditButton').setDisabled(empty);
        this.lookupReference('deviceRemoveButton').setDisabled(empty);
        this.lookupReference('deviceCommandButton').setDisabled(empty);
        if (!empty) {
            this.fireEvent("selectDevice", selected.getLastSelected());
        }
    },

    onUserClick: function() {
        var dialog = Ext.create('Traccar.view.user.UserDialog');
        dialog.down('form').loadRecord(Traccar.app.getUser());
        dialog.show();
    },

    onServerClick: function() {
        var dialog = Ext.create('Traccar.view.ServerDialog');
        dialog.down('form').loadRecord(Traccar.app.getServer());
        dialog.show();
    },

    onUsersClick: function() {
        Ext.create('Ext.window.Window', {
            title: strings.settingsUsers,
            width: styles.windowWidth,
            height: styles.windowHeight,
            layout: 'fit',
            modal: true,
            items: {
                xtype: 'userView'
            }
        }).show();
    },

    selectReport: function(position) {
        if (position !== undefined) {
            this.getView().getSelectionModel().deselectAll();
        }
    }
});
