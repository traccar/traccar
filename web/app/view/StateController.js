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

Ext.define('Traccar.view.StateController', {
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

    init: function () {
        var store = Ext.getStore('LatestPositions');
        store.on('add', this.add, this);
        store.on('update', this.update, this);
    },

    keys: {
        fixTime: {
            priority: 1,
            name: Strings.positionTime
        },
        latitude: {
            priority: 2,
            name: Strings.positionLatitude
        },
        longitude: {
            priority: 3,
            name: Strings.positionLongitude
        },
        valid: {
            priority: 4,
            name: Strings.positionValid
        },
        altitude: {
            priority: 5,
            name: Strings.positionAltitude
        },
        speed: {
            priority: 6,
            name: Strings.positionSpeed
        },
        course: {
            priority: 7,
            name: Strings.positionCourse
        },
        address: {
            priority: 8,
            name: Strings.positionAddress
        },
        protocol: {
            priority: 9,
            name: Strings.positionProtocol
        }
    },

    formatValue: function (value) {
        if (typeof (id) === 'number') {
            return value.toFixed(2);
        } else {
            return value;
        }
    },

    updatePosition: function (position) {
        var attributes, store, key;
        store = Ext.getStore('Attributes');
        store.removeAll();

        for (key in position.data) {
            if (position.data.hasOwnProperty(key) && this.keys[key] !== undefined) {
                store.add(Ext.create('Traccar.model.Attribute', {
                    priority: this.keys[key].priority,
                    name: this.keys[key].name,
                    value: Traccar.AttributeFormatter.getFormatter(key)(position.get(key))
                }));
            }
        }

        attributes = position.get('attributes');
        if (attributes instanceof Object) {
            for (key in attributes) {
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
        }
    },

    selectDevice: function (device) {
        var found;
        this.deviceId = device.get('id');
        found = Ext.getStore('LatestPositions').query('deviceId', this.deviceId);
        if (found.getCount() > 0) {
            this.updatePosition(found.first());
        } else {
            Ext.getStore('Attributes').removeAll();
        }
    },

    add: function (store, data) {
        if (this.deviceId === data[0].get('deviceId')) {
            this.updatePosition(data[0]);
        }
    },

    update: function (store, data) {
        if (this.deviceId === data.get('deviceId')) {
            this.updatePosition(data);
        }
    }
});
