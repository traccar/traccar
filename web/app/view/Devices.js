/*
 * Copyright 2015 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

Ext.define('Traccar.view.Devices', {
    extend: 'Ext.grid.Panel',
    xtype: 'devicesView',

    requires: [
        'Traccar.view.DevicesController',
        'Traccar.view.EditToolbar',
        'Traccar.view.SettingsMenu'
    ],

    controller: 'devices',
    rootVisible: false,

    initComponent: function() {
        this.store = Ext.create('Ext.data.ChainedStore', {
            source: 'Devices'
        });
        this.callParent();
    },

    title: Strings.deviceTitle,
    selType: 'rowmodel',

    tbar: {
        xtype: 'editToolbar',
        items: [{
            disabled: true,
            handler: 'onCommandClick',
            reference: 'deviceCommandButton',
            glyph: 'xf093@FontAwesome',
            tooltip: Strings.deviceCommand,
            tooltipType: 'title'
        }, {
            xtype: 'tbfill'
        }, {
            id: 'deviceFollowButton',
            glyph: 'xf05b@FontAwesome',
            tooltip: Strings.deviceFollow,
            tooltipType: 'title',
            enableToggle: true
        }, {
            xtype: 'settingsMenu'
        }]
    },

    bbar: [{
        xtype: 'tbtext',
        html: Strings.groupParent
    }, {
        xtype: 'combobox',
        store: 'Groups',
        queryMode: 'local',
        displayField: 'name',
        valueField: 'id',
        flex: 1,
        listeners: {
            change: function () {
                if (Ext.isNumber(this.getValue())) {
                    this.up('grid').store.filter({
                        id: 'groupFilter',
                        filterFn: function(item) {
                            var groupId, group, groupStore, filter = true;
                            groupId = item.get('groupId');
                            groupStore = Ext.getStore('Groups');

                            while (groupId) {
                                group = groupStore.getById(groupId);
                                if (group) {
                                    if (group.get('id') === this.getValue()) {
                                        filter = false;
                                        break;
                                    }
                                    groupId = group.get('groupId');
                                } else {
                                    groupId = 0;
                                }
                            }

                            return !filter;
                        },
                        scope: this
                });
                } else {
                    this.up('grid').store.removeFilter('groupFilter');
                }
            }
        }
    }, {
        xtype: 'tbtext',
        html: Strings.sharedSearch
    }, {
        xtype: 'textfield',
        flex: 1,
        listeners: {
            change: function () {
                this.up('grid').store.filter('name', this.getValue());
            }
        }
    }],

    listeners: {
        selectionchange: 'onSelectionChange'
    },

    columns: [{
        text: Strings.sharedName,
        dataIndex: 'name',
        flex: 1
    }, {
        text: Strings.deviceLastUpdate,
        dataIndex: 'lastUpdate',
        flex: 1,
        renderer: function (value, metaData, record) {
            switch (record.get('status')) {
                case 'online':
                    metaData.tdCls = 'status-color-online';
                    break;
                case 'offline':
                    metaData.tdCls = 'status-color-offline';
                    break;
                default:
                    metaData.tdCls = 'status-color-unknown';
                    break;
            }
            if (Traccar.app.getPreference('twelveHourFormat', false)) {
                return Ext.Date.format(value, Traccar.Style.dateTimeFormat12);
            } else {
                return Ext.Date.format(value, Traccar.Style.dateTimeFormat24);
            }
        }
    }]

});
