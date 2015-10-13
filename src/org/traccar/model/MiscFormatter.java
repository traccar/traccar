/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import org.traccar.helper.Log;
import org.traccar.web.JsonConverter;

/**
 * Format extended tracker status
 */
public final class MiscFormatter {

    private MiscFormatter() {
    }

    private static final String XML_ROOT_NODE = "info";

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private static String format(Object value) {
        if (value instanceof Double || value instanceof Float) {
            return DECIMAL_FORMAT.format(value);
        } else {
            return value.toString();
        }
    }

    public static String toXmlString(Map<String, Object> attributes) {
        StringBuilder result = new StringBuilder();

        result.append("<").append(XML_ROOT_NODE).append(">");

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {

            result.append("<").append(entry.getKey()).append(">");
            result.append(format(entry.getValue()));
            result.append("</").append(entry.getKey()).append(">");
        }

        result.append("</").append(XML_ROOT_NODE).append(">");

        return result.toString();
    }

    public static JsonObject toJson(Map<String, Object> attributes) {
        JsonObjectBuilder json = Json.createObjectBuilder();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (entry.getValue() instanceof String) {
                json.add(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                json.add(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof Long) {
                json.add(entry.getKey(), (Long) entry.getValue());
            } else if (entry.getValue() instanceof Double) {
                json.add(entry.getKey(), (Double) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                json.add(entry.getKey(), (Boolean) entry.getValue());
            } else if (entry.getValue() == null) {
                json.add(entry.getKey(), JsonValue.NULL);
            } else {
                json.add(entry.getKey(), JsonConverter.objectToJson(entry.getValue()));
            }
        }

        return json.build();
    }

    public static Map<String, Object> fromJson(JsonObject json) {

        Map<String, Object> attributes = new LinkedHashMap<>();

        for (Map.Entry<String, JsonValue> entry : json.entrySet()) {
            JsonValue.ValueType type = entry.getValue().getValueType();
            switch (type) {
                case STRING:
                    attributes.put(entry.getKey(), ((JsonString) entry.getValue()).getString());
                    break;
                case NUMBER:
                    JsonNumber number = (JsonNumber) entry.getValue();
                    if (number.isIntegral()) {
                        attributes.put(entry.getKey(), number.longValue());
                    } else {
                        attributes.put(entry.getKey(), number.doubleValue());
                    }
                    break;
                case TRUE:
                    attributes.put(entry.getKey(), true);
                    break;
                case FALSE:
                    attributes.put(entry.getKey(), false);
                    break;
                default:
                    Log.warning(new IllegalArgumentException(type.name()));
                    break;
            }
        }

        return attributes;
    }

    public static String toJsonString(Map<String, Object> attributes) {
        return toJson(attributes).toString();
    }

}
