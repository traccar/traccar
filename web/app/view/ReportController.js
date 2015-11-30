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

Ext.define('Traccar.view.ReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.report',

    config: {
        listen: {
            controller: {
                '*': {
                    selectDevice: 'selectDevice',
                    selectReport: 'selectReport'
                }
            }
        }
    },

    onShowClick: function () {
        
        var deviceId, fromDate, fromTime, from, toDate, toTime, to, store, params;

        var serialize = function (obj) {
            var str = [];
            for(var p in obj)
                if (obj.hasOwnProperty(p)) {
                    str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
                }
            return str.join("&");
        }

        exportButton = this.lookupReference('exportButton');
        exportButton.setDisabled(true);
        if (! this.lookupReference('deviceField').isValid()) {
            Ext.toast('Fill required fields');
            return;
        }

        deviceId = this.lookupReference('deviceField').getValue();

        fromDate = this.lookupReference('fromDateField').getValue();
        fromTime = this.lookupReference('fromTimeField').getValue();

        from = new Date(
            fromDate.getFullYear(), fromDate.getMonth(), fromDate.getDate(),
            fromTime.getHours(), fromTime.getMinutes(), fromTime.getSeconds(), fromTime.getMilliseconds());

        toDate = this.lookupReference('toDateField').getValue();
        toTime = this.lookupReference('toTimeField').getValue();

        to = new Date(
            toDate.getFullYear(), toDate.getMonth(), toDate.getDate(),
            toTime.getHours(), toTime.getMinutes(), toTime.getSeconds(), toTime.getMilliseconds());

        params = {
            deviceId: deviceId,
            from: from.toISOString(),
            to: to.toISOString()
        }

        store = Ext.getStore('Positions');
        store.load({
            params: params,
            callback: function(records, operation, success) {
                if (success && records.length > 0) {
                    exportButton.setHref('api/report/csv?' + serialize(params));
                    exportButton.setDisabled(false);
                } 
            }
        });

    },

    onClearClick: function () {
        Ext.getStore('Positions').removeAll();
    },

    onSelectionChange: function (selected) {
        if (selected.getCount() > 0) {
            this.fireEvent('selectReport', selected.getLastSelected(), true);
        }
    },

    selectDevice: function (device) {
        if (device) {
            this.getView().getSelectionModel().deselectAll();
        }
    },

    selectReport: function (position, center) {
        this.getView().getSelectionModel().select([position], false, true);
    }
});
