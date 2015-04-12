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

import java.util.Iterator;
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
    
    private String toXmlString() {
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
    
    private String toJsonString() {
        StringBuilder result = new StringBuilder();
        
        result.append("{");
        
        Iterator<Map.Entry<String, Object> > i = data.entrySet().iterator();
        
        while (i.hasNext()) {
            Map.Entry<String, Object> entry = i.next();
            
            result.append('"').append(entry.getKey()).append('"');
            result.append(':');
            result.append('"').append(entry.getValue()).append('"');
            
            if (i.hasNext()) {
                result.append(',');
            }
        }

        result.append("}");
        
        return result.toString();
    }

    @Override
    public String toString() {
        return toXmlString();
    }

}
