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

Ext.define('Traccar.AttributeFormatter', {
    singleton: true,

    coordinateFormatter: function (value) {
        return value.toFixed(Traccar.Style.coordinatePrecision);
    },

    speedFormatter: function (value) {
        return Ext.getStore('SpeedUnits').formatValue(value, Traccar.app.getPreference('speedUnit'));
    },

    courseFormatter: function (value) {
        var courseValues = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
        return courseValues[Math.floor(value / 45)];
    },

    distanceFormatter: function (value) {
        return Ext.getStore('DistanceUnits').formatValue(value, Traccar.app.getPreference('distanceUnit'));
    },

    alarmFormatter: function (attributes) {
        var value = '';
        if (attributes instanceof Object) {//for Traccar.view.Attributes
            if (attributes.hasOwnProperty('alarm')) {
                value = attributes.alarm;
                if (typeof value === 'boolean') {
                    value = (value ? Ext.Msg.buttonText.yes : Ext.Msg.buttonText.no);
                }
            }
        } else {//for Traccar.view.Report
            value = attributes;
            if (typeof value === 'boolean') {
                value = (value ? Ext.Msg.buttonText.yes : Ext.Msg.buttonText.no);
            }
        }
        return '<span style="color:red;">' + value + '</span>';
    },

    defaultFormatter: function (value) {
        if (typeof value === 'number') {
            return Number(value.toFixed(Traccar.Style.numberPrecision));
        } else if (typeof value === 'boolean') {
            return value ? Ext.Msg.buttonText.yes : Ext.Msg.buttonText.no;
        } else if (value instanceof Date) {
            if (Traccar.app.getPreference('twelveHourFormat', false)) {
                return Ext.Date.format(value, Traccar.Style.dateTimeFormat12);
            } else {
                return Ext.Date.format(value, Traccar.Style.dateTimeFormat24);
            }
        }
        return value;
    },

    getFormatter: function (key) {
        if (key === 'latitude' || key === 'longitude') {
            return this.coordinateFormatter;
        } else if (key === 'speed') {
            return this.speedFormatter;
        } else if (key === 'course') {
            return this.courseFormatter;
        } else if (key === 'distance' || key === 'odometer') {
            return this.distanceFormatter;
        } else if (key === 'alarm') {
            return this.alarmFormatter;
        } else {
            return this.defaultFormatter;
        }
    }
});
