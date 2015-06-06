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

Ext.define('Traccar.view.user.User', {
    extend: 'Ext.grid.Panel',
    xtype: 'user-view',
    
    requires: [
        'Traccar.view.user.UserController'
    ],
    
    controller: 'user',
    store: 'Users',

    selType: 'rowmodel',
    
    tbar: [{
        text: strings.device_add,
        handler: 'onAddClick',
        reference: 'deviceAddButton'
    }, {
        text: strings.device_edit,
        disabled: true,
        handler: 'onEditClick',
        reference: 'userEditButton'
    }, {
        text: strings.device_remove,
        disabled: true,
        handler: 'onRemoveClick',
        reference: 'userRemoveButton'
    }],

    listeners: {
        selectionchange: 'onSelectionChange'
    },

    columns: [
        { text: strings.login_name, dataIndex: 'name', flex: 1 },
        { text: strings.login_email, dataIndex: 'email', flex: 1 },
        { text: strings.login_admin, dataIndex: 'admin', flex: 1 }
    ]

});
