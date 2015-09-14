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

Ext.define('Traccar.view.state.StateController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.state',

    config: {
        listen: {
            controller: {
                '*': {
                    selectDevice: 'selectDevice'
                }
            }
        }
    },

    init: function() {
        var store = Ext.getStore('LiveData');
        store.on('add', this.add, this);
        store.on('update', this.update, this);
    },

    keys: {
        'fixTime': {
            priority: 1,
            name: strings.positionTime
        },
        'latitude': {
            priority: 2,
            name: strings.positionLatitude
        },
        'longitude': {
            priority: 3,
            name: strings.positionLongitude
        },
        'valid': {
            priority: 4,
            name: strings.positionValid
        },
        'altitude': {
            priority: 5,
            name: strings.positionAltitude
        },
        'speed': {
            priority: 6,
            name: strings.positionSpeed
        },
        'course': {
            priority: 7,
            name: strings.positionCourse
        },
        'address': {
            priority: 8,
            name: strings.positionAddress
        },
        'protocol': {
            priority: 9,
            name: strings.positionProtocol
        }
    },

    formatValue: function(value) {
        if (typeof(id) === 'number') {
            return +value.toFixed(2);
        } else {
            return value;
        }
    },

    updatePosition: function(position) {

        var other;
        var value;
        var unit;
        var store = Ext.getStore('Attributes');
        store.removeAll();

        for (var key in position.data) {
            if (position.data.hasOwnProperty(key) && this.keys[key] !== undefined) {
                value = position.get(key);
                if (key === 'speed') {
                    var speedUnits = Ext.getStore('SpeedUnits');
                    unit = Traccar.getApplication().getUser().get('speedUnit') || Traccar.getApplication().getServer().get('speedUnit') || '';
                    value = speedUnits.convert(value, unit) + ' ' + speedUnits.getUnitName(unit);
                } else if (value instanceof Date) {
                    value = Ext.Date.format(value, styles.dateTimeFormat);
                }
                
                store.add(Ext.create('Traccar.model.Attribute', {
                    priority: this.keys[key].priority,
                    name: this.keys[key].name,
                    value: this.formatValue(value)
                }));
            }
        }

        var xml = position.get('other');
        if (typeof xml === 'string' || xml instanceof String) {
            other = this.parseXml(xml);
        } else {
            other = xml;
        }
        for (var key in other) {
            if (other.hasOwnProperty(key)) {

                value = other[key];
                if (key === 'distance' || key === 'odometer') {
                    var distanceUnits = Ext.getStore('DistanceUnits');
                    unit = Traccar.getApplication().getUser().get('distanceUnit') || Traccar.getApplication().getServer().get('distanceUnit') || '';
                    value = distanceUnits.convert(value, unit) + ' ' + distanceUnits.getUnitName(unit);
                }

                store.add(Ext.create('Traccar.model.Attribute', {
                    priority: 999,
                    name: key.replace(/^./, function (match) {
                        return match.toUpperCase();
                    }),
                    value: this.formatValue(value)
                }));
            }
        }
    },

    selectDevice: function(device) {
        this.deviceId = device.get('id');
        var found = Ext.getStore('LiveData').query('deviceId', this.deviceId);
        if (found.getCount() > 0) {
            this.updatePosition(found.first());
        } else {
            Ext.getStore('Attributes').removeAll();
        }
    },

    add: function(store, data) {
        if (this.deviceId === data[0].get('deviceId')) {
            this.updatePosition(data[0]);
        }
    },

    update: function(store, data) {
        if (this.deviceId === data.get('deviceId')) {
            this.updatePosition(data);
        }
    },

    parseXml: function(xml) {
        var dom = null;
        if (window.DOMParser) {
            dom = (new DOMParser()).parseFromString(xml, "text/xml");
        } else if (window.ActiveXObject) {
            dom = new ActiveXObject('Microsoft.XMLDOM');
            dom.async = false;
            dom.loadXML(xml);
        }

        var result = {};
        var length = dom.childNodes[0].childNodes.length;
        for(var i = 0; i < length; i++) {
            var node = dom.childNodes[0].childNodes[i];
            result[node.nodeName] = node.innerHTML; // use textContent in future
        }
        return result;
    }

});
