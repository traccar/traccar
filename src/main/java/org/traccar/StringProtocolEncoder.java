/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import org.traccar.model.Command;

public abstract class StringProtocolEncoder extends BaseProtocolEncoder {

    public StringProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    public interface ValueFormatter {
        String formatValue(String key, Object value);
    }

    protected String formatCommand(Command command, String format, ValueFormatter valueFormatter, String... keys) {

        Object[] values = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            String value = null;
            if (keys[i].equals(Command.KEY_UNIQUE_ID)) {
                value = getUniqueId(command.getDeviceId());
            } else {
                Object object = command.getAttributes().get(keys[i]);
                if (valueFormatter != null) {
                    value = valueFormatter.formatValue(keys[i], object);
                }
                if (value == null && object != null) {
                    value = object.toString();
                }
                if (value == null) {
                    value = "";
                }
            }
            values[i] = value;
        }

        return String.format(format, values);
    }

    protected String formatCommand(Command command, String format, String... keys) {
        return formatCommand(command, format, null, keys);
    }

}
