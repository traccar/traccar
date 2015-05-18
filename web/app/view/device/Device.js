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
    xtype: 'device-view',
    
    requires: [
        'Traccar.view.device.DeviceController'
    ],
    
    controller: 'device',
    store: 'Devices',

    title: strings.device_title,
    selType: 'rowmodel',
    
    tbar: [{
        text:'Add',
        handler: 'onAddClick',
        reference: 'deviceAddButton'
    }, {
        text:'Edit',
        disabled: true,
        handler: 'onEditClick',
        reference: 'deviceEditButton'
    }, {
        text:'Remove',
        disabled: true,
        handler: 'onRemoveClick',
        reference: 'deviceRemoveButton'
    }, {
        xtype: 'tbfill'
    }, {
        text:'Settings'
    }, {
        text:'Logout',
        handler: 'onLogoutClick'
    }],

    listeners: {
        selectionchange: 'onSelectionChange'
    },
    
    columns: [
        { text: strings.device_name, dataIndex: 'name', flex: 1 },
        { text: strings.device_identifier, dataIndex: 'uniqueId', flex: 1 }
    ]

});
