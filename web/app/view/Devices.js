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
    extend: 'Ext.tree.Panel',
    xtype: 'devicesView',

    requires: [
        'Traccar.view.DevicesController',
        'Traccar.view.EditToolbar',
        'Traccar.view.SettingsMenu'
    ],

    controller: 'devices',
    rootVisible: false,
    store: {
        type: 'tree',
        parentIdProperty: 'groupId',
        proxy: {
            type: 'memory',
            reader: {
                type: 'json'
            }
        }
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

    /*bbar: [{
        xtype: 'tbtext',
        html: Strings.sharedSearch
    }, {
        xtype: 'textfield',
        flex: 1,
        listeners: {
            change: function () {
                var tree, expr;
                tree = this.up('treepanel');
                expr = new RegExp(this.getValue(), 'i');
                tree.store.filter({
                    id: 'nameFilter',
                    filterFn: function (node) {
                        var children, len, visible, i;
                        children = node.childNodes;
                        len = children && children.length;
                        visible = node.isLeaf() ? expr.test(node.get('name')) : false;
                        for (i = 0; i < len && !(visible = children[i].get('visible')); i++);
                        return visible;
                    }
                });
            }
        }
    }],*/

    listeners: {
        selectionchange: 'onSelectionChange',
        beforeselect: 'onBeforeSelect'
    },

    columns: [{
        xtype: 'treecolumn',
        text: Strings.sharedName,
        dataIndex: 'name',
        flex: 1
    }, {
        text: Strings.deviceLastUpdate,
        dataIndex: 'lastUpdate',
        flex: 1,
        renderer: function (value, metaData, record) {
            if (record.get('original') instanceof Traccar.model.Device) {
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
        }
    }]

});
