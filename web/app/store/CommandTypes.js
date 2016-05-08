/*
 * Copyright 2016 Gabor Somogyi (gabor.g.somogyi@gmail.com)
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

Ext.define('Traccar.store.CommandTypes', {
    extend: 'Ext.data.Store',
    fields: ['type', 'name'],

    listeners: {
        'beforeload' : function(store) {
            var proxy;
            proxy = store.getProxy();
            proxy.setUrl('/api/commandtypes?deviceId' + proxy.extraParams.deviceId);
        }
    },

    proxy: {
        type: 'rest',
        url: '',
        reader: {
            type: 'json',
            getData: function(data) {
                Ext.each(data, function(entry) {
                    entry.name = entry.type;
                    if (typeof entry.type !== "undefined") {
                        var nameKey = 'command' + entry.type.charAt(0).toUpperCase() + entry.type.slice(1);
                        var name = Strings[nameKey];
                        if (typeof name !== "undefined") {
                            entry.name = name;
                        }
                    }
                });
                return data;
            }
        }
    }
});
