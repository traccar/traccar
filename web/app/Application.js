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

Ext.define('Traccar.Application', {
    extend: 'Ext.app.Application',
    name: 'Traccar',

    requires: [
        'Traccar.Resources',
        'Traccar.ErrorManager'
    ],
    
    models: [
        'Server',
        'User',
        'Device',
        'Position',
        'Attribute',
        'Command'
    ],
    
    stores: [
        'Devices',
        'Positions',
        'LatestPositions',
        'Users',
        'Attributes',
        'MapTypes',
        'DistanceUnits',
        'SpeedUnits',
        'CommandTypes',
        'TimeUnits',
        'Languages'
    ],

    controllers: [
        'Root'
    ],
    
    setUser: function(user) {
        this.user = user;
    },
    
    getUser: function() {
        return this.user;
    },
    
    setServer: function(server) {
        this.server = server;
    },
    
    getServer: function() {
        return this.server;
    },

    getPreference: function(key, defaultValue) {
        return this.getUser().get('distanceUnit') | this.getServer().get('distanceUnit') | defaultValue;
    },

    getRenderer: function(key) {
        if (key === 'latitude' || key === 'longitude') {
            return function(value) {
                return value.toFixed(5);
            }
        } else if (key === 'speed') {
            return function(value) {
                return Ext.getStore('SpeedUnits').formatValue(value, this.getPreference('speedUnit'));
            }
        } else if (key === 'course') {
            return function(value) {
                var directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
                return directions[Math.floor(value / 45)];
            }
        } else if (key === 'distance' || key === 'odometer') {
            return function(value) {
                return Ext.getStore('DistanceUnits').formatValue(value, this.getPreference('distanceUnit'));
            }
        } else {
            return function(value) {
                if (value instanceof Number) {
                    return value.toFixed(2);
                } else if (value instanceof Date) {
                    return Ext.Date.format(value, styles.dateTimeFormat);
                }
                return value;
            }
        }
    }
    
});
