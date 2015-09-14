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

Ext.define('Traccar.view.state.State', {
    extend: 'Ext.grid.Panel',
    xtype: 'stateView',
    
    requires: [
        'Traccar.view.state.StateController'
    ],
    
    controller: 'state',
    store: 'Attributes',

    title: strings.stateTitle,

    columns: [
        { text: strings.stateName, dataIndex: 'name', flex: 1 },
        { text: strings.stateValue, dataIndex: 'value', flex: 1 }
    ]

});
