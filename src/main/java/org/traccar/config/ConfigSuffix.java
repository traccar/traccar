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

import java.util.List;

public abstract class ConfigSuffix<T> {

    protected final String keySuffix;
    protected final List<KeyType> types;
    protected final T defaultValue;

    ConfigSuffix(String keySuffix, List<KeyType> types, T defaultValue) {
        this.keySuffix = keySuffix;
        this.types = types;
        this.defaultValue = defaultValue;
    }

    public abstract ConfigKey<T> withPrefix(String prefix);

}

class StringConfigSuffix extends ConfigSuffix<String> {
    StringConfigSuffix(String key, List<KeyType> types) {
        super(key, types, null);
    }
    StringConfigSuffix(String key, List<KeyType> types, String defaultValue) {
        super(key, types, defaultValue);
    }
    @Override
    public ConfigKey<String> withPrefix(String prefix) {
        return new StringConfigKey(prefix + keySuffix, types, defaultValue);
    }
}

class BooleanConfigSuffix extends ConfigSuffix<Boolean> {
    BooleanConfigSuffix(String key, List<KeyType> types) {
        super(key, types, null);
    }
    BooleanConfigSuffix(String key, List<KeyType> types, Boolean defaultValue) {
        super(key, types, defaultValue);
    }
    @Override
    public ConfigKey<Boolean> withPrefix(String prefix) {
        return new BooleanConfigKey(prefix + keySuffix, types, defaultValue);
    }
}

class IntegerConfigSuffix extends ConfigSuffix<Integer> {
    IntegerConfigSuffix(String key, List<KeyType> types) {
        super(key, types, null);
    }
    IntegerConfigSuffix(String key, List<KeyType> types, Integer defaultValue) {
        super(key, types, defaultValue);
    }
    @Override
    public ConfigKey<Integer> withPrefix(String prefix) {
        return new IntegerConfigKey(prefix + keySuffix, types, defaultValue);
    }
}

class LongConfigSuffix extends ConfigSuffix<Long> {
    LongConfigSuffix(String key, List<KeyType> types) {
        super(key, types, null);
    }
    LongConfigSuffix(String key, List<KeyType> types, Long defaultValue) {
        super(key, types, defaultValue);
    }
    @Override
    public ConfigKey<Long> withPrefix(String prefix) {
        return new LongConfigKey(prefix + keySuffix, types, defaultValue);
    }
}

class DoubleConfigSuffix extends ConfigSuffix<Double> {
    DoubleConfigSuffix(String key, List<KeyType> types) {
        super(key, types, null);
    }
    DoubleConfigSuffix(String key, List<KeyType> types, Double defaultValue) {
        super(key, types, defaultValue);
    }
    @Override
    public ConfigKey<Double> withPrefix(String prefix) {
        return new DoubleConfigKey(prefix + keySuffix, types, defaultValue);
    }
}
