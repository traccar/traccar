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
                    selectDevice: 'selectDevice',
                    selectReport: 'selectReport'
                }
            },
            store: {
                '#LatestPositions': {
                    add: 'updateLatest',
                    update: 'updateLatest'
                },
                '#Positions': {
                    clear: 'clearReport'
                }
            }
        }
    },

    keys: (function () {
        var i, list, result;
        result = {};
        list = ['fixTime', 'latitude', 'longitude', 'valid', 'altitude', 'speed', 'course', 'address', 'protocol'];
        for (i = 0; i < list.length; i++) {
            result[list[i]] = {
                priority: i,
                name: Strings['position' + list[i].replace(/^\w/g, function (s) {
                    return s.toUpperCase();
                })]
            };
        }
        return result;
    }()),

    updateLatest: function (store, data) {
        var i;
        if (!Ext.isArray(data)) {
            data = [data];
        }
        for (i = 0; i < data.length; i++) {
            if (this.deviceId === data[i].get('deviceId')) {
                this.updatePosition(data[i]);
            }
        }
    },

    formatValue: function (value) {
        if (typeof (id) === 'number') {
            return Number(value.toFixed(2));
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
        var position;
        this.deviceId = device.get('id');
        position = Ext.getStore('LatestPositions').findRecord('deviceId', this.deviceId, 0, false, false, true);
        if (position) {
            this.updatePosition(position);
        } else {
            Ext.getStore('Attributes').removeAll();
        }
    },

    selectReport: function (position) {
        this.deviceId = null;
        this.updatePosition(position);
    },

    clearReport: function (store) {
        Ext.getStore('Attributes').removeAll();
    }
});
