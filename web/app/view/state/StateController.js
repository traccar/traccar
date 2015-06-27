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
            name: strings.report_time
        },
        'latitude': {
            priority: 2,
            name: strings.report_latitude
        },
        'longitude': {
            priority: 3,
            name: strings.report_longitude
        },
        'valid': {
            priority: 4,
            name: strings.report_valid
        },
        'altitude': {
            priority: 5,
            name: strings.report_altitude
        },
        'speed': {
            priority: 6,
            name: strings.report_speed
        },
        'course': {
            priority: 7,
            name: strings.report_course
        },
        'protocol': {
            priority: 8,
            name: strings.state_protocol
        }
    },

    updatePosition: function(position) {

        var store = Ext.getStore('Parameters');
        store.removeAll();

        for (var key in position.data) {
            if (position.data.hasOwnProperty(key) && this.keys[key] !== undefined) {
                store.add(Ext.create('Traccar.model.Parameter', {
                    priority: this.keys[key].priority,
                    name: this.keys[key].name,
                    value: position.get(key)
                }));
            }
        }

        var xml = position.get('other');
        var other = this.parseXml(xml);
        for (var key in other) {
            if (other.hasOwnProperty(key)) {
                store.add(Ext.create('Traccar.model.Parameter', {
                    priority: 999,
                    name: key.replace(/^./, function (match) {
                        return match.toUpperCase();
                    }),
                    value: other[key]
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
            Ext.getStore('Parameters').removeAll();
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
