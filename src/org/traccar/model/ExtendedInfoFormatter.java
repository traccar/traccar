/*
 * Copyright 2013 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

/**
 * Format extended tracker status
 */
public class ExtendedInfoFormatter {

    private static final String rootNode = "info";
    
    private final Map<String, Object> data;
    
    public ExtendedInfoFormatter(String protocol) {
        data = new LinkedHashMap<String, Object>();
        data.put("protocol", protocol);
    }

    public void set(String key, Object value) {
        if (value != null) {
            // Exclude empty strings
            if ((value instanceof String) && ((String) value).isEmpty()) {
                return;
            }
            
            data.put(key, value);
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        
        result.append("<").append(rootNode).append(">");
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
         
            result.append("<").append(entry.getKey()).append(">");
            result.append(entry.getValue());
            result.append("</").append(entry.getKey()).append(">");
        }

        result.append("</").append(rootNode).append(">");
        
        return result.toString();
    }

}
