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

Ext.define('Traccar.store.SpeedUnits', {
    extend: 'Ext.data.Store',
    fields: ['key', 'name'],
    data: [
        {'key': 'kmh', 'name': strings.shared_kmh},
        {'key': 'mph', 'name': strings.shared_mph}
    ],

    convert: function(value, unit) {
        switch (unit) {
            case 'kmh':
                return value * 1.852;
            case 'mph':
                return value * 1.15078;
        }
        return value;
    },

    getUnitName: function(unit) {
        if (unit) {
            return this.findRecord('key', unit).get('name');
        } else {
            return '';
        }
    }
});
