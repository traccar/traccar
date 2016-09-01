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

Ext.define('Traccar.view.NotificationsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.notificationsController',

    requires: [
        'Traccar.store.Notifications'
    ],

    init: function () {
        this.userId = this.getView().user.getId();
        this.getView().getStore().load({
            scope: this,
            callback: function (records, operation, success) {
                var notificationsStore = Ext.create('Traccar.store.Notifications');
                notificationsStore.load({
                    params: {
                        userId: this.userId
                    },
                    scope: this,
                    callback: function (records, operation, success) {
                        var i, index, attributes, storeRecord;
                        if (success) {
                            for (i = 0; i < records.length; i++) {
                                index = this.getView().getStore().findExact('type', records[i].get('type'));
                                attributes = records[i].get('attributes');
                                storeRecord = this.getView().getStore().getAt(index);
                                storeRecord.set('attributes', attributes);
                                storeRecord.commit();
                            }
                        }
                    }
                });
            }
        });
    },

    onBeforeCheckChange: function (column, rowIndex, checked, eOpts) {
        var fields, record, data;
        fields = column.dataIndex.split('\.', 2);
        record = this.getView().getStore().getAt(rowIndex);
        data = record.get(fields[0]);
        if (!data[fields[1]]) {
            data[fields[1]] = 'true';
        } else {
            delete data[fields[1]];
        }
        record.set(fields[0], data);
        record.commit();
    },

    onCheckChange: function (column, rowIndex, checked, eOpts) {
        var record = this.getView().getStore().getAt(rowIndex);
        Ext.Ajax.request({
            scope: this,
            url: 'api/users/notifications',
            jsonData: {
                userId: this.userId,
                type: record.get('type'),
                attributes: record.get('attributes')
            },
            callback: function (options, success, response) {
                if (!success) {
                    Traccar.app.showError(response);
                }
            }
        });
    }
});
