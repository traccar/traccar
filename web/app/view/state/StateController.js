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
        var store = Ext.getStore('LatestPositions');
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

        var attributes;
        var value;
        var unit;
        var store = Ext.getStore('Attributes');
        store.removeAll();

        for (var key in position.data) {
            if (position.data.hasOwnProperty(key) && this.keys[key] !== undefined) {
                store.add(Ext.create('Traccar.model.Attribute', {
                    priority: this.keys[key].priority,
                    name: this.keys[key].name,
                    value: Traccar.AttributeFormatter.getFormatter(key)(position.get(key))
                }));
            }
        }

        var xml = position.get('attributes');
        if (typeof xml === 'string' || xml instanceof String) {
            attributes = this.parseXml(xml);
        } else {
            attributes = xml;
        }
        for (var key in attributes) {
            if (attributes.hasOwnProperty(key)) {
                store.add(Ext.create('Traccar.model.Attribute', {
                    priority: 1024,
                    name: key.replace(/^./, function (match) {
                        return match.toUpperCase();
                    }),
                    value: Traccar.AttributeFormatter.getFormatter(key)(attributes[key])
                }));
            }
        }
    },

    selectDevice: function(device) {
        this.deviceId = device.get('id');
        var found = Ext.getStore('LatestPositions').query('deviceId', this.deviceId);
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
