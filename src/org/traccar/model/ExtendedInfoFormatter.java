/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

/**
 * Format extended tracker status
 */
public class ExtendedInfoFormatter {

    private static final String rootNode = "info";
    
    private StringBuilder data;

    public ExtendedInfoFormatter(String protocol) {
        data = new StringBuilder();
        data.append("<").append(rootNode).append(">");
        data.append("<protocol>").append(protocol).append("</protocol>");
    }

    public void set(String key, Object value) {
        if (value != null) {
            data.append("<").append(key).append(">");
            data.append(value);
            data.append("</").append(key).append(">");
        }
    }

    @Override
    public String toString() {
        return data.toString() + "</" + rootNode + ">";
    }

}
