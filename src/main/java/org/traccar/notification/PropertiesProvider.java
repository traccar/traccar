/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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

import org.traccar.config.Config;
import org.traccar.config.ConfigKey;
import org.traccar.model.ExtendedModel;

public class PropertiesProvider {

    private Config config;

    private ExtendedModel extendedModel;

    public PropertiesProvider(Config config) {
        this.config = config;
    }

    public PropertiesProvider(ExtendedModel extendedModel) {
        this.extendedModel = extendedModel;
    }

    public String getString(ConfigKey<String> key) {
        if (config != null) {
            return config.getString(key);
        } else {
            String result = extendedModel.getString(key.getKey());
            return result != null ? result : key.getDefaultValue();
        }
    }

    public int getInteger(ConfigKey<Integer> key) {
        if (config != null) {
            return config.getInteger(key);
        } else {
            Object result = extendedModel.getAttributes().get(key.getKey());
            if (result != null) {
                return result instanceof String stringResult ? Integer.parseInt(stringResult) : (Integer) result;
            } else {
                return key.getDefaultValue();
            }
        }
    }

    public Boolean getBoolean(ConfigKey<Boolean> key) {
        if (config != null) {
            if (config.hasKey(key)) {
                return config.getBoolean(key);
            } else {
                return null;
            }
        } else {
            Object result = extendedModel.getAttributes().get(key.getKey());
            if (result != null) {
                return result instanceof String stringResult ? Boolean.valueOf(stringResult) : (Boolean) result;
            } else {
                return null;
            }
        }
    }

}
