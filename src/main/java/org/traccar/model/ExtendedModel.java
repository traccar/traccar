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
package org.traccar.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ExtendedModel extends BaseModel {

    private Map<String, Object> attributes = new LinkedHashMap<>();

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = Objects.requireNonNullElseGet(attributes, LinkedHashMap::new);
    }

    public void set(String key, Boolean value) {
        if (value != null) {
            attributes.put(key, value);
        }
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

    public String getString(String key, String defaultValue) {
        return parseAsString(attributes.get(key), defaultValue);
    }

    public String getString(String key) {
        return parseAsString(attributes.get(key), null);
    }

    public double getDouble(String key) {
        return parseAsDouble(attributes.get(key), 0.0);
    }

    public boolean getBoolean(String key) {
        return parseAsBoolean(attributes.get(key), false);
    }

    public int getInteger(String key) {
        return parseAsInteger(attributes.get(key), 0);
    }

    public long getLong(String key) {
        return parseAsLong(attributes.get(key), 0L);
    }

    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    public String removeString(String key) {
        return parseAsString(attributes.remove(key), null);
    }

    public Double removeDouble(String key) {
        return parseAsDouble(attributes.remove(key), null);
    }

    public Boolean removeBoolean(String key) {
        return parseAsBoolean(attributes.remove(key), null);
    }

    public Integer removeInteger(String key) {
        return parseAsInteger(attributes.remove(key), null);
    }

    public Long removeLong(String key) {
        return parseAsLong(attributes.remove(key), null);
    }

    private String parseAsString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            return value.toString();
        }
    }

    private static Double parseAsDouble(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Number numberValue) {
            return numberValue.doubleValue();
        } else {
            return Double.parseDouble(value.toString());
        }
    }

    private static Boolean parseAsBoolean(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Boolean booleanValue) {
            return booleanValue;
        } else {
            return Boolean.parseBoolean(value.toString());
        }
    }

    private static Integer parseAsInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Number numberValue) {
            return numberValue.intValue();
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    private static Long parseAsLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Number numberValue) {
            return numberValue.longValue();
        } else {
            return Long.parseLong(value.toString());
        }
    }
}
