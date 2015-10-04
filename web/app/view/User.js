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

Ext.define('Traccar.view.User', {
    extend: 'Ext.grid.Panel',
    xtype: 'userView',

    requires: [
        'Traccar.view.UserController',
        'Traccar.view.EditToolbar'
    ],

    controller: 'user',
    store: 'Users',

    selType: 'rowmodel',

    tbar: {
        xtype: 'editToolbar',
        items: [{
            text: Strings.deviceTitle,
            disabled: true,
            handler: 'onDevicesClick',
            reference: 'userDevicesButton'
        }]
    },

    listeners: {
        selectionchange: 'onSelectionChange'
    },

    columns: [{
        text: Strings.userName,
        dataIndex: 'name',
        flex: 1
    }, {
        text: Strings.userEmail,
        dataIndex: 'email',
        flex: 1
    }, {
        text: Strings.userAdmin,
        dataIndex: 'admin',
        flex: 1
    }]
});
