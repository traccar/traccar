/*
 * Copyright 2019 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ConfigKey<T> {

    private final String key;
    private final Set<KeyType> types = new HashSet<>();
    private final Class<T> valueClass;
    private final T defaultValue;

    ConfigKey(String key, List<KeyType> types, Class<T> valueClass, T defaultValue) {
        this.key = key;
        this.types.addAll(types);
        this.valueClass = valueClass;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public boolean hasType(KeyType type) {
        return types.contains(type);
    }

    public Class<T> getValueClass() {
        return valueClass;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

}

class StringConfigKey extends ConfigKey<String> {
    StringConfigKey(String key, List<KeyType> types) {
        super(key, types, String.class, null);
    }
    StringConfigKey(String key, List<KeyType> types, String defaultValue) {
        super(key, types, String.class, defaultValue);
    }
}

class BooleanConfigKey extends ConfigKey<Boolean> {
    BooleanConfigKey(String key, List<KeyType> types) {
        super(key, types, Boolean.class, null);
    }
    BooleanConfigKey(String key, List<KeyType> types, Boolean defaultValue) {
        super(key, types, Boolean.class, defaultValue);
    }
}

class IntegerConfigKey extends ConfigKey<Integer> {
    IntegerConfigKey(String key, List<KeyType> types) {
        super(key, types, Integer.class, null);
    }
    IntegerConfigKey(String key, List<KeyType> types, Integer defaultValue) {
        super(key, types, Integer.class, defaultValue);
    }
}

class LongConfigKey extends ConfigKey<Long> {
    LongConfigKey(String key, List<KeyType> types) {
        super(key, types, Long.class, null);
    }
    LongConfigKey(String key, List<KeyType> types, Long defaultValue) {
        super(key, types, Long.class, defaultValue);
    }
}

class DoubleConfigKey extends ConfigKey<Double> {
    DoubleConfigKey(String key, List<KeyType> types) {
        super(key, types, Double.class, null);
    }
    DoubleConfigKey(String key, List<KeyType> types, Double defaultValue) {
        super(key, types, Double.class, defaultValue);
    }
}
