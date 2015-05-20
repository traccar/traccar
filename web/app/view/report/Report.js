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
    xtype: 'report-view',

    requires: [
        'Traccar.view.report.ReportController'
    ],

    controller: 'report',
    store: 'Positions',

    title: strings.report_title,

    tbar: [{
        xtype: 'tbtext',
        html: strings.report_device
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
        html: strings.report_from
    }, {
        xtype: 'datefield',
        reference: 'fromDateField',
        value: new Date()
    }, {
        xtype: 'timefield',
        reference: 'fromTimeField',
        maxWidth: styles.report_time,
        value: new Date()
    }, '-', {
        xtype: 'tbtext',
        html: strings.report_to
    }, {
        xtype: 'datefield',
        reference: 'toDateField',
        value: new Date()
    }, {
        xtype: 'timefield',
        reference: 'toTimeField',
        maxWidth: styles.report_time,
        value: new Date()
    }, '-', {
        text: strings.report_show,
        handler: 'onShowClick'
    }, {
        text: strings.report_clear,
        handler: 'onClearClick'
    }],

    columns: [
        { text: strings.report_valid, dataIndex: 'valid', flex: 1 },
        { text: strings.report_time, dataIndex: 'fixTime', flex: 1, xtype: 'datecolumn', format: styles.report_format },
        { text: strings.report_latitude, dataIndex: 'latitude', flex: 1 },
        { text: strings.report_longitude, dataIndex: 'longitude', flex: 1 },
        { text: strings.report_altitude, dataIndex: 'altitude', flex: 1 },
        { text: strings.report_speed, dataIndex: 'speed', flex: 1 },
        { text: strings.report_course, dataIndex: 'course', flex: 1 },
        { text: strings.report_address, dataIndex: 'address', flex: 1 }
    ]

});
