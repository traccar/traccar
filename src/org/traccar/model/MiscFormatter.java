/*
 * Copyright 2013 - 2016 Anton Tananaev (anton@traccar.org)
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
import java.util.Map;

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

}
