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

Ext.define('Traccar.view.GroupGeofencesController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.groupGeofences',

    init: function () {
        var admin = Traccar.app.getUser().get('admin');
        this.groupId = this.getView().group.getData().id;
        this.getView().setStore(Ext.getStore((admin) ? 'AllGeofences' : 'Geofences'));
        this.getView().getStore().load({
            scope: this,
            callback: function (records, operation, success) {
                var groupStore = Ext.create((admin) ? 'Traccar.store.AllGeofences' : 'Traccar.store.Geofences');
                groupStore.load({
                    params: {
                        groupId: this.groupId
                    },
                    scope: this,
                    callback: function (records, operation, success) {
                        var i, index;
                        if (success) {
                            for (i = 0; i < records.length; i++) {
                                index = this.getView().getStore().find('id', records[i].getData().id);
                                this.getView().getSelectionModel().select(index, true, true);
                            }
                        }
                    }
                });
            }
        });
    },

    onBeforeSelect: function (object, record, index) {
        Ext.Ajax.request({
            scope: this,
            url: '/api/groups/geofences',
            jsonData: {
                groupId: this.groupId,
                geofenceId: record.getData().id
            },
            callback: function (options, success, response) {
                if (!success) {
                    Traccar.app.showError(response);
                }
            }
        });
    },

    onBeforeDeselect: function (object, record, index) {
        Ext.Ajax.request({
            scope: this,
            method: 'DELETE',
            url: '/api/groups/geofences',
            jsonData: {
                groupId: this.groupId,
                geofenceId: record.getData().id
            },
            callback: function (options, success, response) {
                if (!success) {
                    Traccar.app.showError(response);
                }
            }
        });
    }
});
