/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

Ext.define('Traccar.view.GeofencesController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.geofences',

    requires: [
        'Traccar.view.GeofenceDialog',
        'Traccar.model.Geofence'
    ],

    init: function () {
        Ext.getStore('Geofences').load();
    },

    onAddClick: function () {
        var geofence, dialog;
        geofence = Ext.create('Traccar.model.Geofence');
        geofence.store = this.getView().getStore();
        dialog = Ext.create('Traccar.view.GeofenceDialog');
        dialog.down('form').loadRecord(geofence);
        dialog.show();
    },

    onEditClick: function () {
        var geofence, dialog;
        geofence = this.getView().getSelectionModel().getSelection()[0];
        dialog = Ext.create('Traccar.view.GeofenceDialog');
        dialog.down('form').loadRecord(geofence);
        dialog.show();
    },

    onRemoveClick: function () {
        var geofence = this.getView().getSelectionModel().getSelection()[0];
        Ext.Msg.show({
            title: Strings.sharedGeofence,
            message: Strings.sharedRemoveConfirm,
            buttons: Ext.Msg.YESNO,
            buttonText: {
                yes: Strings.sharedRemove,
                no: Strings.sharedCancel
            },
            fn: function (btn) {
                var store = Ext.getStore('Geofences');
                if (btn === 'yes') {
                    store.remove(geofence);
                    store.sync();
                }
            }
        });
    },

    onSelectionChange: function (selected) {
        var disabled = selected.length > 0;
        this.lookupReference('toolbarEditButton').setDisabled(disabled);
        this.lookupReference('toolbarRemoveButton').setDisabled(disabled);
    }
});
