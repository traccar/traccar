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

Ext.define('Traccar.view.report.Report', {
    extend: 'Ext.grid.Panel',
    xtype: 'reportView',

    requires: [
        'Traccar.view.report.ReportController'
    ],

    controller: 'report',
    store: 'Positions',

    title: strings.reportTitle,

    tbar: [{
        xtype: 'tbtext',
        html: strings.reportDevice
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
        html: strings.reportFrom
    }, {
        xtype: 'datefield',
        reference: 'fromDateField',
        value: new Date()
    }, {
        xtype: 'timefield',
        reference: 'fromTimeField',
        maxWidth: styles.reportTime,
        value: new Date()
    }, '-', {
        xtype: 'tbtext',
        html: strings.reportTo
    }, {
        xtype: 'datefield',
        reference: 'toDateField',
        value: new Date()
    }, {
        xtype: 'timefield',
        reference: 'toTimeField',
        maxWidth: styles.reportTime,
        value: new Date()
    }, '-', {
        text: strings.reportShow,
        handler: 'onShowClick'
    }, {
        text: strings.reportClear,
        handler: 'onClearClick'
    }],

    listeners: {
        selectionchange: 'onSelectionChange'
    },

    columns: [
        { text: strings.positionValid, dataIndex: 'valid', flex: 1 },
        { text: strings.positionTime, dataIndex: 'fixTime', flex: 1, xtype: 'datecolumn', format: styles.reportFormat },
        { text: strings.positionLatitude, dataIndex: 'latitude', flex: 1 },
        { text: strings.positionLongitude, dataIndex: 'longitude', flex: 1 },
        { text: strings.positionAltitude, dataIndex: 'altitude', flex: 1 },
        {
            text: strings.positionSpeed,
            dataIndex: 'speed',
            flex: 1,
            renderer: function(value) {
                var speedUnits = Ext.getStore('SpeedUnits');
                var unit = Traccar.getApplication().getUser().get('speedUnit') || Traccar.getApplication().getServer().get('speedUnit') || '';
                return speedUnits.convert(value, unit) + ' ' + speedUnits.getUnitName(unit);
            }
        },
        { text: strings.positionAddress, dataIndex: 'address', flex: 1 }
    ]

});
