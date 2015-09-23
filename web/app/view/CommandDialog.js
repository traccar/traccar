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

Ext.define('Traccar.view.CommandDialog', {
    extend: 'Traccar.view.BaseDialog',

    requires: [
        'Traccar.view.CommandDialogController'
    ],

    controller: 'commandDialog',
    title: strings.commandTitle,

    items: {
        xtype: 'form',
        items: [{
            xtype: 'combobox',
            name: 'type',
            fieldLabel: strings.commandType,
            store: 'CommandTypes',
            displayField: 'name',
            valueField: 'key',
            listeners: {
                select: 'onSelect'
            }
        }, {
            xtype: 'fieldcontainer',
            reference: 'paramPositionPeriodic',
            name: 'attributes',
            hidden: true,

            items: [{
                xtype: 'numberfield',
                fieldLabel: strings.commandFrequency,
                name: 'frequency'
            }, {
                xtype: 'combobox',
                fieldLabel: strings.commandUnit,
                name: 'unit',
                store: 'TimeUnits',
                displayField: 'name',
                valueField: 'factor'
            }]
        }]
    },

    buttons: [{
        text: strings.commandSend,
        handler: 'onSendClick'
    }, {
        text: strings.sharedCancel,
        handler: 'onCancelClick'
    }]
});
