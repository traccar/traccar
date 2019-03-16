/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
package org.traccar.config;

public class ConfigKey {

    private final String key;
    private final Class clazz;

    ConfigKey(String key, Class clazz) {
        this.key = key;
        this.clazz = clazz;
    }

    String getKey() {
        return key;
    }

    Class getValueClass() {
        return clazz;
    }

}
