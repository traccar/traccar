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

Ext.define('DeviceView', {
    extend: 'Ext.grid.Panel',
    xtype: 'device-view',

    title: Strings.device_title,
    
    tbar: [{
        text:'Add',
        handler: function() {
            
            var device = {
                name: "AjaxTest",
                uniqueId: "UniqueId"
            };
            
            Ext.Ajax.request({
                url: '/api/device/add',
                jsonData: Ext.encode(device),
                success: function() {
                    alert("success");
                }
            });
        }
    }, {
        text:'Edit'
    }, {
        text:'Remove'
    }, {
        xtype: 'tbfill'
    }, {
        text:'Settings'
    }, {
        text:'Logout',
        handler: function() {
            Ext.Ajax.request({
                url: '/api/logout',
                success: function() {
                    window.location.reload();
                }
            });
        }
    }],
    
    store: {
        proxy: {
            type: 'ajax',
            url: '/api/device/get',
            reader: {
                type: 'json',
                rootProperty: 'data'
            }
        },
        autoLoad: true,

        fields:[
            'id',
            'name',
            'uniqueId',
            'positionId',
            'dataId'
        ]
    },
    
    columns: [
        { text: Strings.device_name,  dataIndex: 'name', flex: 1 },
        { text: Strings.device_identifier, dataIndex: 'uniqueId', flex: 1 }
    ]
});
