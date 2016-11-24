/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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
package org.traccar.notification;

import org.traccar.Config;
import org.traccar.model.Extensible;

public class PropertiesProvider {

    private Config config;

    private Extensible extensible;

    public PropertiesProvider(Config config) {
        this.config = config;
    }

    public PropertiesProvider(Extensible extensible) {
        this.extensible = extensible;
    }

    public String getString(String key) {
        if (config != null) {
            return config.getString(key);
        } else {
            return extensible.getString(key);
        }
    }

    public String getString(String key, String defaultValue) {
        String value = getString(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

}
