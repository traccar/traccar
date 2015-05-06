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

import org.traccar.helper.Log;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Format extended tracker status
 */
public class MiscFormatter {

    private static final String xmlRootNode = "info";

    private static DecimalFormat decimalFormat = new DecimalFormat("#.##");

    private static String format(Object value) {
        if (value instanceof Double || value instanceof Float) {
            return decimalFormat.format(value);
        } else {
            return value.toString();
        }
    }

    public static String toXmlString(Map<String, Object> other) {
        StringBuilder result = new StringBuilder();
        
        result.append("<").append(xmlRootNode).append(">");
        
        for (Map.Entry<String, Object> entry : other.entrySet()) {
         
            result.append("<").append(entry.getKey()).append(">");
            result.append(format(entry.getValue()));
            result.append("</").append(entry.getKey()).append(">");
        }

        result.append("</").append(xmlRootNode).append(">");
        
        return result.toString();
    }

    public static JsonObject toJson(Map<String, Object> other) {
        JsonObjectBuilder json = Json.createObjectBuilder();

        for (Map.Entry<String, Object> entry : other.entrySet()) {
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
            } else {
                Log.warning("Unknown type of data");
            }
        }

        return json.build();
    }
    
    private static String toJsonString(Map<String, Object> other) {
        return toJson(other).toString();
    }

}
