/*
 * Copyright 2016 Gabor Somogyi (gabor.g.somogyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class H02ProtocolEncoder extends StringProtocolEncoder {

    private Object formatCommand(DateTime dt, String uniqueId, String cmd, String... params) {

        String result = String.format(
                "*HQ,%s,%s,%02d%02d%02d",
                uniqueId,
                cmd,
                dt.getHourOfDay(),
                dt.getMinuteOfHour(),
                dt.getSecondOfMinute()
        );

        for(String param : params) {
            result += "," + param;
        }

        result += "#";

        return result;
    }

    protected Object encodeCommand(Command command, DateTime dt) {
        String uniqueId = getUniqueId(command.getDeviceId());

        switch (command.getType()) {
            case Command.TYPE_ALARM_ARM:
                return formatCommand(dt, uniqueId, "SCF", "0", "0");
            case Command.TYPE_ALARM_DISARM:
                return formatCommand(dt, uniqueId, "SCF", "1", "1");
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

    @Override
    protected Object encodeCommand(Command command) {
        DateTime dt = new DateTime(DateTimeZone.UTC);
        return encodeCommand(command, dt);
    }
}
