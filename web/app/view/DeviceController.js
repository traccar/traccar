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

Ext.define('Traccar.view.DeviceController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.device',

    requires: [
        'Traccar.view.CommandDialog',
        'Traccar.view.DeviceDialog',
        'Traccar.view.UserDialog',
        'Traccar.view.User'
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

    onAddClick: function () {
        var device, dialog;
        device = Ext.create('Traccar.model.Device');
        device.store = this.getView().getStore();
        dialog = Ext.create('Traccar.view.DeviceDialog');
        dialog.down('form').loadRecord(device);
        dialog.show();
    },

    onEditClick: function () {
        var device, dialog;
        device = this.getView().getSelectionModel().getSelection()[0];
        dialog = Ext.create('Traccar.view.DeviceDialog');
        dialog.down('form').loadRecord(device);
        dialog.show();
    },

    onRemoveClick: function () {
        var device = this.getView().getSelectionModel().getSelection()[0];
        Ext.Msg.show({
            title: Strings.deviceDialog,
            message: Strings.sharedRemoveConfirm,
            buttons: Ext.Msg.YESNO,
            buttonText: {
                yes: Strings.sharedRemove,
                no: Strings.sharedCancel
            },
            fn: function (btn) {
                var store;
                if (btn === 'yes') {
                    store = Ext.getStore('Devices');
                    store.remove(device);
                    store.sync();
                }
            }
        });
    },

    onCommandClick: function () {
        var device, command, dialog;
        device = this.getView().getSelectionModel().getSelection()[0];
        command = Ext.create('Traccar.model.Command');
        command.set('deviceId', device.get('id'));
        dialog = Ext.create('Traccar.view.CommandDialog');
        dialog.down('form').loadRecord(command);
        dialog.show();
    },

    onSelectionChange: function (selected) {
        var empty = selected.getCount() === 0;
        this.lookupReference('toolbarEditButton').setDisabled(empty);
        this.lookupReference('toolbarRemoveButton').setDisabled(empty);
        this.lookupReference('deviceCommandButton').setDisabled(empty);
        if (!empty) {
            this.fireEvent('selectDevice', selected.getLastSelected());
        }
    },

    selectReport: function (position) {
        if (position !== undefined) {
            this.getView().getSelectionModel().deselectAll();
        }
    }
});
