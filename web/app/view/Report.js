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

Ext.define('Traccar.view.Report', {
    extend: 'Ext.grid.Panel',
    xtype: 'reportView',

    requires: [
        'Traccar.view.ReportController',
        'Traccar.view.CustomTimeField'
    ],

    controller: 'report',
    store: 'Positions',

    title: getString('reportTitle'),

    tbar: [{
        xtype: 'tbtext',
        html: getString('reportDevice')
    }, {
        xtype: 'combobox',
        reference: 'deviceField',
        store: 'Devices',
        valueField: 'id',
        displayField: 'name',
        typeAhead: true,
        queryMode: 'local'
    }, '-', {
        xtype: 'tbtext',
        html: getString('reportFrom')
    }, {
        xtype: 'datefield',
        reference: 'fromDateField',
        startDay: Traccar.Style.weekStartDay,
        format: Traccar.Style.dateFormat,
        value: new Date(new Date().getTime() - 30 * 60 * 1000)
    }, {
        xtype: 'customTimeField',
        reference: 'fromTimeField',
        maxWidth: Traccar.Style.reportTime,
        value: new Date(new Date().getTime() - 30 * 60 * 1000)
    }, '-', {
        xtype: 'tbtext',
        html: getString('reportTo')
    }, {
        xtype: 'datefield',
        reference: 'toDateField',
        startDay: Traccar.Style.weekStartDay,
        format: Traccar.Style.dateFormat,
        value: new Date()
    }, {
        xtype: 'customTimeField',
        reference: 'toTimeField',
        maxWidth: Traccar.Style.reportTime,
        value: new Date()
    }, '-', {
        text: getString('reportShow'),
        handler: 'onShowClick'
    }, {
        text: getString('reportClear'),
        handler: 'onClearClick'
    }],

    listeners: {
        selectionchange: 'onSelectionChange'
    },

    columns: [{
        text: getString('positionValid'),
        dataIndex: 'valid',
        flex: 1,
        renderer: Traccar.AttributeFormatter.getFormatter('valid')
    }, {
        text: getString('positionFixTime'),
        dataIndex: 'fixTime',
        flex: 1,
        xtype: 'datecolumn',
        renderer: Traccar.AttributeFormatter.getFormatter('fixTime')
    }, {
        text: getString('positionLatitude'),
        dataIndex: 'latitude',
        flex: 1,
        renderer: Traccar.AttributeFormatter.getFormatter('latitude')
    }, {
        text: getString('positionLongitude'),
        dataIndex: 'longitude',
        flex: 1,
        renderer: Traccar.AttributeFormatter.getFormatter('latitude')
    }, {
        text: getString('positionAltitude'),
        dataIndex: 'altitude',
        flex: 1,
        renderer: Traccar.AttributeFormatter.getFormatter('altitude')
    }, {
        text: getString('positionSpeed'),
        dataIndex: 'speed',
        flex: 1,
        renderer: Traccar.AttributeFormatter.getFormatter('speed')
    }, {
        text: getString('positionAddress'),
        dataIndex: 'address',
        flex: 1,
        renderer: Traccar.AttributeFormatter.getFormatter('address')
    }]
});
