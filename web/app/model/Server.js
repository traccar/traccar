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

Ext.define('Traccar.model.Server', {
    extend: 'Ext.data.Model',
    identifier: 'negative',

    fields: [{
        name: 'id',
        type: 'int'
    }, {
        name: 'registration',
        type: 'boolean'
    }, {
        name: 'map',
        type: 'string'
    }, {
        name: 'bingKey',
        type: 'string'
    }, {
        name: 'mapUrl',
        type: 'string'
    }, {
        name: 'language',
        type: 'string'
    }, {
        name: 'distanceUnit',
        type: 'string'
    }, {
        name: 'speedUnit',
        type: 'string'
    }, {
        name: 'latitude',
        type: 'float'
    }, {
        name: 'longitude',
        type: 'float'
    }, {
        name: 'zoom',
        type: 'int'
    }],

    proxy: {
        type: 'ajax',
        url: '/api/server/update',
        writer: {
            type: 'json',
            writeAllFields: true
        }
    }
});
