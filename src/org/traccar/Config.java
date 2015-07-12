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
package org.traccar;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private final Properties properties = new Properties();
    
    public void load(String file) throws IOException {
        properties.loadFromXML(new FileInputStream(file));
    }
    
    public boolean hasKey(String key) {
        return properties.containsKey(key);
    }
    
    public boolean getBoolean(String key) {
        return Boolean.valueOf(properties.getProperty(key));
    }
    
    public int getInteger(String key) {
        return getInteger(key, 0);
    }
    
    public int getInteger(String key, int defaultValue) {
        if (properties.containsKey(key)) {
            return Integer.valueOf(properties.getProperty(key));
        } else {
            return defaultValue;
        }
    }
    
    public long getLong(String key) {
        return getLong(key, 0);
    }
    
    public long getLong(String key, long defaultValue) {
        if (properties.containsKey(key)) {
            return Long.valueOf(properties.getProperty(key));
        } else {
            return defaultValue;
        }
    }
    
    public String getString(String key) {
        return properties.getProperty(key);
    }
    
    public String getString(String key, String defaultValue) {
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        } else {
            return defaultValue;
        }
    }
    
}
