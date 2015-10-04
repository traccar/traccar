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

Ext.define('Traccar.view.MainMobile', {
    extend: 'Ext.container.Viewport',
    alias: 'widget.mainMobile',

    requires: [
        'Traccar.view.Device',
        'Traccar.view.State',
        'Traccar.view.Map'
    ],

    layout: 'border',

    defaults: {
        header: false,
        collapsible: true,
        split: true
    },

    items: [{
        region: 'east',
        xtype: 'stateView',
        flex: 4
    }, {
        region: 'center',
        xtype: 'mapView',
        collapsible: false,
        flex: 2
    }, {
        region: 'south',
        xtype: 'deviceView',
        flex: 1
    }]
});
