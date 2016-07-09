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

Ext.define('Traccar.view.Notifications', {
    extend: 'Ext.grid.Panel',
    xtype: 'notificationsView',

    requires: [
        'Traccar.view.NotificationsController'
    ],

    controller: 'notificationsController',
    store: 'AllNotifications',

    selModel: {
        selType: 'cellmodel'
    },

    viewConfig: {
        markDirty:false
    },

    columns: [{
        text: Strings.notificationType,
        dataIndex: 'type',
        flex: 1,
        renderer: function (value) {
            var typeKey = 'event' + value.charAt(0).toUpperCase() + value.slice(1);
            return Strings[typeKey];
        }
    }, {
        text: Strings.notificationWeb,
        dataIndex: 'attributes.web',
        xtype: 'checkcolumn',
        flex: 1,
        listeners: {
            beforeCheckChange: 'onBeforeCheckChange',
            checkChange: 'onCheckChange'
        },
        renderer: function (value, metaData, record) {
            var fields = this.dataIndex.split('\.', 2);
            return (new Ext.ux.CheckColumn()).renderer(record.get(fields[0])[fields[1]], metaData);
        }
    }, {
        text: Strings.notificationMail,
        dataIndex: 'attributes.mail',
        xtype: 'checkcolumn',
        flex: 1,
        listeners: {
            beforeCheckChange: 'onBeforeCheckChange',
            checkChange: 'onCheckChange'
        },
        renderer: function (value, metaData, record) {
            var fields = this.dataIndex.split('\.', 2);
            return (new Ext.ux.CheckColumn()).renderer(record.get(fields[0])[fields[1]], metaData);
        }
    }]
});
