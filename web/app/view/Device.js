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
(function () {
    'use strict';

    Ext.define('Traccar.view.Device', {
        extend: 'Ext.grid.Panel',
        xtype: 'deviceView',

        requires: [
            'Traccar.view.DeviceController'
        ],

        controller: 'device',
        store: 'Devices',

        title: Strings.deviceTitle,
        selType: 'rowmodel',

        tbar: [{
            handler: 'onAddClick',
            reference: 'deviceAddButton',
            glyph: 'xf067@FontAwesome',
            tooltip: Strings.sharedAdd,
            tooltipType: 'title'
        }, {
            disabled: true,
            handler: 'onEditClick',
            reference: 'deviceEditButton',
            glyph: 'xf040@FontAwesome',
            tooltip: Strings.sharedEdit,
            tooltipType: 'title'
        }, {
            disabled: true,
            handler: 'onRemoveClick',
            reference: 'deviceRemoveButton',
            glyph: 'xf00d@FontAwesome',
            tooltip: Strings.sharedRemove,
            tooltipType: 'title'
        }, {
            disabled: true,
            handler: 'onCommandClick',
            reference: 'deviceCommandButton',
            glyph: 'xf093@FontAwesome',
            tooltip: Strings.deviceCommand,
            tooltipType: 'title'
        }, {
            xtype: 'tbfill'
        }, {
            text: Strings.settingsTitle,
            menu: [{
                text: Strings.settingsUser,
                handler: 'onUserClick'
            }, {
                text: Strings.settingsServer,
                hidden: true,
                handler: 'onServerClick',
                reference: 'settingsServerButton'
            }, {
                text: Strings.settingsUsers,
                hidden: true,
                handler: 'onUsersClick',
                reference: 'settingsUsersButton'
            }]
        }, {
            text: Strings.loginLogout,
            handler: 'onLogoutClick'
        }],

        listeners: {
            selectionchange: 'onSelectionChange'
        },

        columns: [{
            text: Strings.deviceName,
            dataIndex: 'name', flex: 1
        }, {
            text: Strings.deviceIdentifier,
            dataIndex: 'uniqueId', flex: 1
        }]

    });

})();
