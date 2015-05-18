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

Ext.define('Traccar.model.Position', {
    extend: 'Ext.data.Model',
    identifier: 'negative',

    fields: [
        { name: 'id', type: 'int' },
        { name: 'protocol', type: 'string' },
        { name: 'deviceId', type: 'int' },
        { name: 'serverTime', type: 'date' },
        { name: 'deviceTime', type: 'date' },
        { name: 'fixTime', type: 'date' },
        { name: 'valid', type: 'boolean' },
        { name: 'latitude', type: 'float' },
        { name: 'longitude', type: 'float' },
        { name: 'altitude', type: 'float' },
        { name: 'speed', type: 'float' },
        { name: 'course', type: 'float' },
        { name: 'address', type: 'string' },
        { name: 'other', type: 'string' }
    ]
});
