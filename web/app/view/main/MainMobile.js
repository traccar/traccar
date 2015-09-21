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

Ext.define('Traccar.view.main.MainMobile', {
    extend: 'Ext.container.Viewport',

    requires: [
        'Traccar.view.device.Device',
        'Traccar.view.state.State',
        'Traccar.view.map.Map'
    ],

    layout: 'border',

    defaults: {
        header: false,
        collapsible: true,
        split: true
    },

    items: [{
        xtype: 'stateView',
        region: 'east',
        collapsed:true,
        flex: 4
    }, {
        xtype: 'mapView',
        region: 'center',
        collapsible: false,
        flex: 2
    }, {
        xtype: 'deviceView',
        region: 'south',
        flex: 1
    }]
});
