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
    fields: ['key', 'name', 'factor'],

    data: [{
        key: 'kmh',
        name: Strings.sharedKmh,
        factor: 1.852
    }, {
        key: 'mph',
        name: Strings.sharedMph,
        factor: 1.15078
    }],

    formatValue: function (value, unit) {
        var model;
        if (unit) {
            model = this.findRecord('key', unit);
            return (value * model.get('factor')).toFixed(1) + ' ' + model.get('name');
        } else {
            return value;
        }
    }
});
