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

Ext.define('Traccar.view.report.ReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.report',

    config: {
        listen: {
            controller: {
                '*': {
                    selectDevice: 'selectDevice'
                }
            }
        }
    },

    onShowClick: function() {
        var deviceId = this.lookupReference('deviceField').getValue();

        var fromDate = this.lookupReference('fromDateField').getValue();
        var fromTime = this.lookupReference('fromTimeField').getValue();

        var from = new Date(
            fromDate.getFullYear(), fromDate.getMonth(), fromDate.getDate(),
            fromTime.getHours(), fromTime.getMinutes(), fromTime.getSeconds(), fromTime.getMilliseconds());

        var toDate = this.lookupReference('toDateField').getValue();
        var toTime = this.lookupReference('toTimeField').getValue();

        var to = new Date(
            toDate.getFullYear(), toDate.getMonth(), toDate.getDate(),
            toTime.getHours(), toTime.getMinutes(), toTime.getSeconds(), toTime.getMilliseconds());

        var store = Ext.getStore('Positions');
        store.load({
            params:{
                deviceId: deviceId,
                from: from,
                to: to
            },
            scope: this,
            callback: function() {
                this.fireEvent("reportShow");
            }
        });
    },

    onClearClick: function() {
        Ext.getStore('Positions').removeAll();
        this.fireEvent("reportClear");
    },

    onSelectionChange: function(selected) {
        if (selected.getCount() > 0) {
            this.fireEvent("selectReport", selected.getLastSelected());
        }
    },

    selectDevice: function(device) {
        if (device !== undefined) {
            this.getView().getSelectionModel().deselectAll();
        }
    }

});
