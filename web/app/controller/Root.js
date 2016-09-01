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

Ext.define('Traccar.controller.Root', {
    extend: 'Ext.app.Controller',

    requires: [
        'Traccar.view.Login',
        'Traccar.view.Main',
        'Traccar.view.MainMobile',
        'Traccar.model.Position'
    ],

    init: function () {
        var indicator = document.createElement('div');
        indicator.className = 'state-indicator';
        document.body.appendChild(indicator);
        this.isPhone = parseInt(window.getComputedStyle(indicator).getPropertyValue('z-index'), 10) !== 0;
    },

    onLaunch: function () {
        Ext.Ajax.request({
            scope: this,
            url: 'api/server',
            callback: this.onServerReturn
        });
    },

    onServerReturn: function (options, success, response) {
        Ext.get('spinner').remove();
        if (success) {
            Traccar.app.setServer(Ext.decode(response.responseText));
            Ext.Ajax.request({
                scope: this,
                url: 'api/session',
                callback: this.onSessionReturn
            });
        } else {
            Traccar.app.showError(response);
        }
    },

    onSessionReturn: function (options, success, response) {
        if (success) {
            Traccar.app.setUser(Ext.decode(response.responseText));
            this.loadApp();
        } else {
            this.login = Ext.create('widget.login', {
                listeners: {
                    scope: this,
                    login: this.onLogin
                }
            });
            this.login.show();
        }
    },

    onLogin: function () {
        this.login.close();
        this.loadApp();
    },

    loadApp: function () {
        var attribution;
        Ext.getStore('Groups').load();
        Ext.getStore('Geofences').load();
        Ext.getStore('Devices').load({
            scope: this,
            callback: function () {
                this.asyncUpdate(true);
            }
        });
        attribution = Ext.get('attribution');
        if (attribution) {
            attribution.remove();
        }
        if (this.isPhone) {
            Ext.create('widget.mainMobile');
        } else {
            Ext.create('widget.main');
        }
    },

    beep: function () {
        if (!this.beepSound) {
            this.beepSound = new Audio('beep.wav');
        }
        this.beepSound.play();
    },

    mutePressed: function () {
        var muteButton = Ext.getCmp('muteButton');
        return muteButton && !muteButton.pressed;
    },

    asyncUpdate: function (first) {
        var protocol, socket, self = this;
        protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        socket = new WebSocket(protocol + '//' + window.location.host + window.location.pathname + 'api/socket');

        socket.onclose = function (event) {
            self.asyncUpdate(false);
        };

        socket.onmessage = function (event) {
            var i, j, store, data, array, entity, device, typeKey, alarmKey, text, geofence;

            data = Ext.decode(event.data);

            if (data.devices) {
                array = data.devices;
                store = Ext.getStore('Devices');
                for (i = 0; i < array.length; i++) {
                    entity = store.getById(array[i].id);
                    if (entity) {
                        entity.set({
                            status: array[i].status,
                            lastUpdate: array[i].lastUpdate
                        }, {
                            dirty: false
                        });
                    }
                }
            }

            if (data.positions && !data.events) {
                array = data.positions;
                store = Ext.getStore('LatestPositions');
                for (i = 0; i < array.length; i++) {
                    entity = store.findRecord('deviceId', array[i].deviceId, 0, false, false, true);
                    if (entity) {
                        entity.set(array[i]);
                    } else {
                        store.add(Ext.create('Traccar.model.Position', array[i]));
                    }
                }
            }

            if (data.events) {
                array = data.events;
                store = Ext.getStore('Events');
                for (i = 0; i < array.length; i++) {
                    store.add(array[i]);
                    if (array[i].type === 'commandResult' && data.positions) {
                        for (j = 0; j < data.positions.length; j++) {
                            if (data.positions[j].id === array[i].positionId) {
                                text = data.positions[j].attributes.result;
                                break;
                            }
                        }
                        text = Strings.eventCommandResult + ': ' + text;
                    } else if (array[i].type === 'alarm' && data.positions) {
                        alarmKey = 'alarm';
                        text = Strings[alarmKey];
                        if (!text) {
                            text = alarmKey;
                        }
                        for (j = 0; j < data.positions.length; j++) {
                            if (data.positions[j].id === array[i].positionId && data.positions[j].attributes.alarm !== null) {
                                if (typeof data.positions[j].attributes.alarm === 'string' && data.positions[j].attributes.alarm.length >= 2) {
                                    alarmKey = 'alarm' + data.positions[j].attributes.alarm.charAt(0).toUpperCase() + data.positions[j].attributes.alarm.slice(1);
                                    text = Strings[alarmKey];
                                    if (!text) {
                                        text = alarmKey;
                                    }
                                }
                                break;
                            }
                        }
                    } else {
                        typeKey = 'event' + array[i].type.charAt(0).toUpperCase() + array[i].type.slice(1);
                        text = Strings[typeKey];
                        if (!text) {
                            text = typeKey;
                        }
                    }
                    if (array[i].geofenceId !== 0) {
                        geofence = Ext.getStore('Geofences').getById(array[i].geofenceId);
                        if (typeof geofence !== 'undefined') {
                            text += ' \"' + geofence.get('name') + '"';
                        }
                    }
                    device = Ext.getStore('Devices').getById(array[i].deviceId);
                    if (typeof device !== 'undefined') {
                        if (self.mutePressed()) {
                            self.beep();
                        }
                        Ext.toast(text, device.get('name'));
                    }
                }
            }
        };
    }
});
