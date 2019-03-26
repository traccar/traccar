/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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

import java.util.Map;

public abstract class StringProtocolEncoder extends BaseProtocolEncoder {

    public interface ValueFormatter {
        String formatValue(String key, Object value);
    }

    protected String formatCommand(Command command, String format, ValueFormatter valueFormatter, String... keys) {

        String result = String.format(format, (Object[]) keys);

        result = result.replaceAll("\\{" + Command.KEY_UNIQUE_ID + "}", getUniqueId(command.getDeviceId()));
        for (Map.Entry<String, Object> entry : command.getAttributes().entrySet()) {
            String value = null;
            if (valueFormatter != null) {
                value = valueFormatter.formatValue(entry.getKey(), entry.getValue());
            }
            if (value == null) {
                value = entry.getValue().toString();
            }
            result = result.replaceAll("\\{" + entry.getKey() + "}", value);
        }

        return result;
    }

    protected String formatCommand(Command command, String format, String... keys) {
        return formatCommand(command, format, null, keys);
    }

}
