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

Ext.define('Traccar.view.UserDevicesController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.userDevices',

    init: function () {
        this.userId = this.getView().user.getData().id;
        this.getView().getStore().load({
            scope: this,
            callback: function (records, operation, success) {
                var userStore = Ext.create('Traccar.store.Devices');

                userStore.load({
                    params: {
                        userId: this.userId
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
            url: '/api/device/link',
            params: {
                userId: this.userId,
                deviceId: record.getData().id
            },
            callback: Traccar.app.getErrorHandler(this, function (options, success, response) {
                if (!success) {
                    // TODO deselect again
                }
            })
        });
    },

    onBeforeDeselect: function (object, record, index) {
        Ext.Ajax.request({
            scope: this,
            url: '/api/device/unlink',
            params: {
                userId: this.userId,
                deviceId: record.getData().id
            },
            callback: Traccar.app.getErrorHandler(this, function (options, success, response) {
                if (!success) {
                    // TODO select again
                }
            })
        });
    }
});
