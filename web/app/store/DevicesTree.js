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

Ext.define('Traccar.store.DevicesTree', {
    extend: 'Ext.data.TreeStore',

    parentIdProperty: 'groupId',

    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },

    constructor: function () {
        this.callParent(arguments);

        Ext.getStore('Groups').on({
            scope: this,
            load: this.onGroupLoad,
            update: this.onGroupUpdate
        });

        Ext.getStore('Devices').on({
            scope: this,
            load: this.onDeviceLoad,
            update: this.onDeviceUpdate
        });
    },

    reloadData: function () {
        var groupsStore, devicesStore, nodes = [];
        groupsStore = Ext.getStore('Groups');
        devicesStore = Ext.getStore('Devices');

        groupsStore.each(function (record) {
            var groupId, node = {
                id: 'g' + record.get('id'),
                original: record,
                name: record.get('name'),
                leaf: true
            };
            groupId = record.get('groupId');
            if (groupId !== 0 && groupsStore.indexOfId(groupId) !== -1) {
                node.groupId = 'g' + groupId;
            }
            nodes.push(node);
        }, this);
        devicesStore.each(function (record) {
            var groupId, node = {
                id: 'd' + record.get('id'),
                original: record,
                name: record.get('name'),
                status: record.get('status'),
                lastUpdate: record.get('lastUpdate'),
                leaf: true
            };
            groupId = record.get('groupId');
            if (groupId !== 0 && groupsStore.indexOfId(groupId) !== -1) {
                node.groupId = 'g' + groupId;
            }
            nodes.push(node);
        }, this);

        this.getProxy().setData(nodes);
        this.load();
    },

    onGroupLoad: function () {
        console.log('onGroupLoad');
        this.reloadData();
    },

    onDeviceLoad: function () {
        console.log('onDeviceLoad');
        this.reloadData();
    },

    onGroupUpdate: function () {
        console.log('onGroupUpdate');
    },

    onDeviceUpdate: function () {
        console.log('onDeviceUpdate');
    }
});
