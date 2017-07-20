/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
package org.traccar.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExtendedModel extends BaseModel {

    private Map<String, Object> attributes = new LinkedHashMap<>();

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void set(String key, Boolean value) {
        attributes.put(key, value);
    }

    public void set(String key, Byte value) {
        if (value != null) {
            attributes.put(key, value.intValue());
        }
    }

    public void set(String key, Short value) {
        if (value != null) {
            attributes.put(key, value.intValue());
        }
    }

    public void set(String key, Integer value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }

    public void set(String key, Long value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }

    public void set(String key, Float value) {
        if (value != null) {
            attributes.put(key, value.doubleValue());
        }
    }

    public void set(String key, Double value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }

    public void set(String key, String value) {
        if (value != null && !value.isEmpty()) {
            attributes.put(key, value);
        }
    }

    public void add(Map.Entry<String, Object> entry) {
        if (entry != null && entry.getValue() != null) {
            attributes.put(entry.getKey(), entry.getValue());
        }
    }

    public String getString(String key) {
        if (attributes.containsKey(key)) {
            return (String) attributes.get(key);
        } else {
            return null;
        }
    }

    public double getDouble(String key) {
        if (attributes.containsKey(key)) {
            return ((Number) attributes.get(key)).doubleValue();
        } else {
            return 0.0;
        }
    }

    public boolean getBoolean(String key) {
        if (attributes.containsKey(key)) {
            return Boolean.parseBoolean(attributes.get(key).toString());
        } else {
            return false;
        }
    }

    public int getInteger(String key) {
        if (attributes.containsKey(key)) {
            return ((Number) attributes.get(key)).intValue();
        } else {
            return 0;
        }
    }

    public long getLong(String key) {
        if (attributes.containsKey(key)) {
            return ((Number) attributes.get(key)).longValue();
        } else {
            return 0;
        }
    }

}
