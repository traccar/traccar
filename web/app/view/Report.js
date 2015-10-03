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

    Ext.define('Traccar.view.Report', {
        extend: 'Ext.grid.Panel',
        xtype: 'reportView',

        requires: [
            'Traccar.view.ReportController'
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
            startDay: Traccar.Style.weekStartDay,
            value: new Date(new Date().getTime() - 30 * 60 * 1000)
        }, {
            xtype: 'timefield',
            reference: 'fromTimeField',
            maxWidth: Traccar.Style.reportTime,
            format: Traccar.Style.timeFormat,
            value: new Date(new Date().getTime() - 30 * 60 * 1000)
        }, '-', {
            xtype: 'tbtext',
            html: strings.reportTo
        }, {
            xtype: 'datefield',
            reference: 'toDateField',
            startDay: Traccar.Style.weekStartDay,
            value: new Date()
        }, {
            xtype: 'timefield',
            reference: 'toTimeField',
            maxWidth: Traccar.Style.reportTime,
            format: Traccar.Style.timeFormat,
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

        columns: [{
            text: strings.positionValid,
            dataIndex: 'valid',
            flex: 1,
            renderer: Traccar.AttributeFormatter.getFormatter('valid')
        }, {
            text: strings.positionTime,
            dataIndex: 'fixTime',
            flex: 1,
            xtype: 'datecolumn',
            renderer: Traccar.AttributeFormatter.getFormatter('fixTime')
        }, {
            text: strings.positionLatitude,
            dataIndex: 'latitude',
            flex: 1,
            renderer: Traccar.AttributeFormatter.getFormatter('latitude')
        }, {
            text: strings.positionLongitude,
            dataIndex: 'longitude',
            flex: 1,
            renderer: Traccar.AttributeFormatter.getFormatter('latitude')
        }, {
            text: strings.positionAltitude,
            dataIndex: 'altitude',
            flex: 1,
            renderer: Traccar.AttributeFormatter.getFormatter('altitude')
        }, {
            text: strings.positionSpeed,
            dataIndex: 'speed',
            flex: 1,
            renderer: Traccar.AttributeFormatter.getFormatter('speed')
        }, {
            text: strings.positionAddress,
            dataIndex: 'address',
            flex: 1,
            renderer: Traccar.AttributeFormatter.getFormatter('address')
        }]
    });

})();
