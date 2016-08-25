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

Ext.define('Traccar.view.Report', {
    extend: 'Ext.grid.Panel',
    xtype: 'reportView',

    requires: [
        'Traccar.view.ReportController',
        'Traccar.view.CustomTimeField'
    ],

    controller: 'report',

    title: Strings.reportTitle,

    tbar: [{
        xtype: 'tbtext',
        html: Strings.sharedType
    }, {
        xtype: 'combobox',
        reference: 'reportTypeField',
        store: 'ReportTypes',
        displayField: 'name',
        valueField: 'key',
        typeAhead: true,
        listeners: {
            change: 'onTypeChange'
        }
    }, '-', {
        xtype: 'tbtext',
        html: Strings.reportDevice
    }, {
        xtype: 'tagfield',
        reference: 'deviceField',
        store: 'Devices',
        valueField: 'id',
        displayField: 'name',
        queryMode: 'local'
    }, '-', {
        xtype: 'tbtext',
        html: Strings.reportFrom
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
        html: Strings.reportTo
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
        text: Strings.reportShow,
        reference: 'showButton',
        handler: 'onReportClick'
    }, {
        text: Strings.reportCsv,
        reference: 'csvButton',
        handler: 'onReportClick'
    }, {
        text: Strings.reportClear,
        handler: 'onClearClick'
    }],

    listeners: {
        selectionchange: 'onSelectionChange'
    }
});
