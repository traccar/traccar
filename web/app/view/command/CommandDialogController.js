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

Ext.define('Traccar.view.command.CommandDialogController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.commanddialog',

    onSelect: function(selected) {
        this.lookupReference('paramPositionFix').setHidden(selected.getValue() !== 'positionFix');
    },

    onSendClick: function(button) {
        var other;
        var form = button.up('window').down('form');
        form.updateRecord();
        var record = form.getRecord();

        if (record.get('type') === 'positionPeriodic') {
            other = this.lookupReference('paramPositionPeriodic');
            var value = other.down('numberfield[name="frequency"]').getValue();
            value *= other.down('combobox[name="unit"]').getValue();

            record.set('other', {
                frequency: value
            });
        }

        Ext.Ajax.request({
            scope: this,
            url: '/api/command/send',
            jsonData: record.getData(),
            callback: this.onSendReturn
        });
    },

    onSendReturn: function(options, success, response) {
        if (Traccar.ErrorManager.check(success, response)) {
            this.closeView();
            //TODO toast Ext.toast(strings.login_created);
        }
    },

    onCancelClick: function(button) {
        button.up('window').close();
    }

});
