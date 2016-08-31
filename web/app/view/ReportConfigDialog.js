/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 * Copyright 2016 Andrey Kunitsyn (abyss@fox5.ru)
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

Ext.define('Traccar.view.ReportConfigDialog', {
    extend: 'Traccar.view.BaseDialog',

    requires: [
        'Traccar.view.ReportConfigController',
        'Traccar.view.CustomTimeField'
    ],

    controller: 'reportConfigDialog',
    title: Strings.reportConfigure,

    items: [{
        fieldLabel: Strings.reportDevice,
        xtype: 'tagfield',
        width: Traccar.Style.reportTagfieldWidth,
        reference: 'deviceField',
        store: 'Devices',
        valueField: 'id',
        displayField: 'name',
        queryMode: 'local'
    }, {
        fieldLabel: Strings.reportGroup,
        xtype: 'tagfield',
        width: Traccar.Style.reportTagfieldWidth,
        reference: 'groupField',
        store: 'Groups',
        valueField: 'id',
        displayField: 'name',
        queryMode: 'local'
    }, {
        fieldLabel: Strings.reportEventTypes,
        xtype: 'tagfield',
        width: Traccar.Style.reportTagfieldWidth,
        reference: 'eventTypeField',
        store: 'ReportEventTypes',
        hidden: true,
        valueField: 'type',
        displayField: 'name',
        queryMode: 'local'
    }, {
        xtype: 'fieldcontainer',
        layout: 'hbox',
        items: [{
            xtype: 'datefield',
            fieldLabel: Strings.reportFrom,
            reference: 'fromDateField',
            startDay: Traccar.Style.weekStartDay,
            format: Traccar.Style.dateFormat,
            value: new Date(new Date().getTime() - 30 * 60 * 1000)
        }, {
            xtype: 'customTimeField',
            reference: 'fromTimeField',
            maxWidth: Traccar.Style.reportTime,
            value: new Date(new Date().getTime() - 30 * 60 * 1000)
        }]
    }, {
        xtype: 'fieldcontainer',
        layout: 'hbox',
        items: [{
            xtype: 'datefield',
            fieldLabel: Strings.reportTo,
            reference: 'toDateField',
            startDay: Traccar.Style.weekStartDay,
            format: Traccar.Style.dateFormat,
            value: new Date()
        }, {
            xtype: 'customTimeField',
            reference: 'toTimeField',
            maxWidth: Traccar.Style.reportTime,
            value: new Date()
        }]
    }],

    buttons: [{
        text: Strings.sharedSave,
        handler: 'onSaveClick'
    }, {
        text: Strings.sharedCancel,
        handler: 'closeView'
    }]
});
