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
        'Traccar.view.ReportController'
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
        text: Strings.reportConfigure,
        handler: 'onConfigureClick'
    }, '-', {
        text: Strings.reportShow,
        reference: 'showButton',
        disabled: true,
        handler: 'onReportClick'
    }, {
        text: Strings.reportCsv,
        reference: 'csvButton',
        disabled: true,
        handler: 'onReportClick'
    }, {
        text: Strings.reportClear,
        handler: 'onClearClick'
    }],

    listeners: {
        selectionchange: 'onSelectionChange'
    }
});
