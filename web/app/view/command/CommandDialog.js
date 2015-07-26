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

Ext.define('Traccar.view.command.CommandDialog', {
    extend: 'Ext.window.Window',
    xtype: 'command-dialog',

    requires: [
        'Traccar.view.command.CommandDialogController'
    ],

    controller: 'commanddialog',
    
    bodyPadding: styles.panel_padding,
    title: strings.command_title,
    resizable: false,
    modal: true,
    
    items: {
        xtype: 'form',
        items: [{
            xtype: 'combobox',
            name: 'type',
            fieldLabel: strings.command_type,
            store: 'CommandTypes',
            displayField: 'name',
            valueField: 'key',
            listeners: {
                select: 'onSelect'
            }
        }, {
            xtype: 'fieldcontainer',
            reference: 'paramPositionFix',
            name: 'other',
            hidden: true,

            items: [{
                xtype: 'numberfield',
                fieldLabel: strings.command_frequency,
                name: 'frequency'
            }, {
                xtype: 'combobox',
                fieldLabel: strings.command_unit,
                name: 'unit',
                store: 'TimeUnits',
                displayField: 'name',
                valueField: 'multiplier'
            }]
        }]
    },

    buttons: [{
        text: strings.command_send,
        handler: 'onSendClick'
    }, {
        text: strings.shared_cancel,
        handler: 'onCancelClick'
    }]

});
