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

public final class Keys {

    public static final ConfigKey FORWARD_ENABLE = new ConfigKey(
            "forward.enable",
            Boolean.class,
            "Enable positions forwarding to other web server.");

    public static final ConfigKey FORWARD_URL = new ConfigKey(
            "forward.url",
            String.class,
            "URL to forward positions. Data is passed through URL parameters. For example, {uniqueId} for device "
                    + "identifier, {latitude} and {longitude} for coordinates.");

    public static final ConfigKey FORWARD_HEADER = new ConfigKey(
            "forward.header",
            String.class,
            "Additional HTTP header, can be used for authorization.");

    public static final ConfigKey FORWARD_JSON = new ConfigKey(
            "forward.json",
            Boolean.class,
            "Boolean value to enable forwarding in JSON format.");

    private Keys() {
    }

}
