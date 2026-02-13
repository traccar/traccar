/*
 * Copyright 2015 - 2024 Anton Tananaev (anton@traccar.org)
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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.name.Named;
import org.traccar.helper.Log;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Objects;
import java.util.Properties;

@Singleton
public class Config {

    private final Properties properties = new Properties();

    private boolean useEnvironmentVariables;

    public Config() {
    }

    @Inject
    public Config(@Named("configFile") String file) throws IOException {
        try {
            try (InputStream inputStream = new FileInputStream(file)) {
                properties.loadFromXML(inputStream);
            }

            useEnvironmentVariables = Boolean.parseBoolean(System.getenv("CONFIG_USE_ENVIRONMENT_VARIABLES"))
                    || Boolean.parseBoolean(properties.getProperty("config.useEnvironmentVariables"));

            Log.setupLogger(this);
        } catch (InvalidPropertiesFormatException e) {
            Log.setupDefaultLogger();
            throw new RuntimeException("Configuration file is not a valid XML document", e);
        } catch (Exception e) {
            Log.setupDefaultLogger();
            throw e;
        }
    }

    public boolean hasKey(ConfigKey<?> key) {
        return hasKey(key.getKey());
    }

    private boolean hasKey(String key) {
        return useEnvironmentVariables && System.getenv().containsKey(getEnvironmentVariableName(key))
                || properties.containsKey(key);
    }

    public String getString(ConfigKey<String> key) {
        return getString(key.getKey(), key.getDefaultValue());
    }

    @Deprecated
    public String getString(String key) {
        if (useEnvironmentVariables) {
            String value = System.getenv(getEnvironmentVariableName(key));
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return properties.getProperty(key);
    }

    public String getString(ConfigKey<String> key, String defaultValue) {
        return getString(key.getKey(), defaultValue);
    }

    @Deprecated
    public String getString(String key, String defaultValue) {
        return hasKey(key) ? getString(key) : defaultValue;
    }

    public boolean getBoolean(ConfigKey<Boolean> key) {
        String value = getString(key.getKey());
        if (value != null) {
            return Boolean.parseBoolean(value);
        } else {
            Boolean defaultValue = key.getDefaultValue();
            return Objects.requireNonNullElse(defaultValue, false);
        }
    }

    public int getInteger(ConfigKey<Integer> key) {
        String value = getString(key.getKey());
        if (value != null) {
            return Integer.parseInt(value);
        } else {
            Integer defaultValue = key.getDefaultValue();
            return Objects.requireNonNullElse(defaultValue, 0);
        }
    }

    public int getInteger(ConfigKey<Integer> key, int defaultValue) {
        return getInteger(key.getKey(), defaultValue);
    }

    @Deprecated
    public int getInteger(String key, int defaultValue) {
        return hasKey(key) ? Integer.parseInt(getString(key)) : defaultValue;
    }

    public long getLong(ConfigKey<Long> key) {
        String value = getString(key.getKey());
        if (value != null) {
            return Long.parseLong(value);
        } else {
            Long defaultValue = key.getDefaultValue();
            return Objects.requireNonNullElse(defaultValue, 0L);
        }
    }

    public double getDouble(ConfigKey<Double> key) {
        String value = getString(key.getKey());
        if (value != null) {
            return Double.parseDouble(value);
        } else {
            Double defaultValue = key.getDefaultValue();
            return Objects.requireNonNullElse(defaultValue, 0.0);
        }
    }

    @VisibleForTesting
    public void setString(ConfigKey<?> key, String value) {
        properties.put(key.getKey(), value);
    }

    static String getEnvironmentVariableName(String key) {
        return key.replaceAll("\\.", "_").replaceAll("(\\p{Lu})", "_$1").toUpperCase();
    }

}
