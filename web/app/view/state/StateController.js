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
        store.on('add', this.add);
        store.on('update', this.update);
    },

    keys: {
        'fixTime': strings.report_time,
        'latitude': strings.report_latitude,
        'longitude': strings.report_longitude,
        'valid': strings.report_valid,
        'altitude': strings.report_altitude,
        'speed': strings.report_speed,
        'course': strings.report_course,
        'protocol': strings.state_protocol
    },

    selectDevice: function(device) {
        var position = {
            "fixTime":"2012-01-02T01:50:00",
            "longitude":130.0,
            "latitude":60.0,
            "valid":true,
            "altitude":0.0,
            "speed":0.0,
            "course":0.0,
            "deviceId":1,
            "other":"<info><status>84-20</status></info>",
            "deviceTime":"2012-01-02T01:50:00",
            "id":29,
            "protocol":"gotop"
        };

        var store = Ext.getStore('Parameters');
        store.removeAll();

        for (var key in position) {
            if (position.hasOwnProperty(key) && this.keys[key] !== undefined) {
                store.add(Ext.create('Traccar.model.Parameter', {
                    name: this.keys[key],
                    value: position[key]
                }));
            }
        }
    },

    add: function(store, data) {
        console.log(data);
    },

    update: function(store, data) {
        console.log(data);
    }

});
