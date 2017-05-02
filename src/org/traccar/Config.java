/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class Config {

    private final Properties properties = new Properties();

    void load(String file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            properties.loadFromXML(inputStream);
        }

        String defaultConfigFile = properties.getProperty("config.default");
        if (defaultConfigFile != null) {
            try (InputStream inputStream = new FileInputStream(defaultConfigFile)) {
                Properties defaultProperties = new Properties();
                defaultProperties.loadFromXML(inputStream);

                Enumeration props = defaultProperties.propertyNames();
                while (props.hasMoreElements()) {
                    String key = (String) props.nextElement();
                    if (!properties.containsKey(key)) {
                        properties.setProperty(key, defaultProperties.getProperty(key));
                    }
                }
            }
        }
    }

    public boolean hasKey(String key) {
        return properties.containsKey(key);
    }

    public String getString(String key) {
        String envName = key.toUpperCase().replaceAll("\\.", "_");
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return properties.getProperty(key);
    }

    public String getString(String key, String defaultValue) {
        return hasKey(key) ? getString(key) : defaultValue;
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    public int getInteger(String key) {
        return getInteger(key, 0);
    }

    public int getInteger(String key, int defaultValue) {
        return hasKey(key) ? Integer.parseInt(getString(key)) : defaultValue;
    }

    public long getLong(String key) {
        return getLong(key, 0);
    }

    public long getLong(String key, long defaultValue) {
        return hasKey(key) ? Long.parseLong(getString(key)) : defaultValue;
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) {
        return hasKey(key) ? Double.parseDouble(getString(key)) : defaultValue;
    }

}
