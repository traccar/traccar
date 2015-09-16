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

Ext.define('Traccar.view.device.Device', {
    extend: 'Ext.grid.Panel',
    xtype: 'deviceView',

    requires: [
        'Traccar.view.device.DeviceController'
    ],
    
    controller: 'device',
    store: 'Devices',

    title: strings.deviceTitle,
    selType: 'rowmodel',
    
    tbar: [{
        handler: 'onAddClick',
        reference: 'deviceAddButton',
        glyph: 'xf067@FontAwesome',
        tooltip: strings.sharedAdd,
        tooltipType: 'title'
    }, {
        disabled: true,
        handler: 'onEditClick',
        reference: 'deviceEditButton',
        glyph: 'xf040@FontAwesome',
        tooltip: strings.sharedEdit,
        tooltipType: 'title'
    }, {
        disabled: true,
        handler: 'onRemoveClick',
        reference: 'deviceRemoveButton',
        glyph: 'xf00d@FontAwesome',
        tooltip: strings.sharedRemove,
        tooltipType: 'title'
    }, {
        disabled: true,
        handler: 'onCommandClick',
        reference: 'deviceCommandButton',
        glyph: 'xf093@FontAwesome',
        tooltip: strings.deviceCommand,
        tooltipType: 'title'
    }, {
        xtype: 'tbfill'
    }, {
        text: strings.settingsTitle,
        menu: [{
            text: strings.settingsUser,
            handler: 'onUserClick'
        }, {
            text: strings.settingsServer,
            hidden: true,
            handler: 'onServerClick',
            reference: 'settingsServerButton'
        }, {
            text: strings.settingsUsers,
            hidden: true,
            handler: 'onUsersClick',
            reference: 'settingsUsersButton'
        }]
    }, {
        text: strings.loginLogout,
        handler: 'onLogoutClick'
    }],

    listeners: {
        selectionchange: 'onSelectionChange'
    },
    
    columns: [{
        text: strings.deviceName,
        dataIndex: 'name', flex: 1
    }, {
        text: strings.deviceIdentifier,
        dataIndex: 'uniqueId', flex: 1
    }]

});
