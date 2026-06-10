/*
 * Copyright 2026 Drew Taylor (Drew.Taylor@fognetx.com)
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
package org.traccar.protocol;

import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class MictrackHQProtocolEncoder extends BaseProtocolEncoder {

    private static final DateTimeFormatter UTC_TIME = DateTimeFormatter
            .ofPattern("HHmmss").withZone(ZoneOffset.UTC);

    public MictrackHQProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private String utcNow() {
        return UTC_TIME.format(Instant.now());
    }

    private String format(Command command, String cmd, String... params) {
        String id = getUniqueId(command.getDeviceId());
        String base = String.format("*HQ,%s,%s,%s", id, cmd, utcNow());
        if (params.length > 0) {
            base += "," + String.join(",", params);
        }
        return base + "#";
    }

    @Override
    protected Object encodeCommand(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_CUSTOM ->
                    format(command, (String) command.getAttributes().get(Command.KEY_DATA));
            case Command.TYPE_POSITION_PERIODIC -> {
                int interval = ((Number) command.getAttributes().get(Command.KEY_FREQUENCY)).intValue();
                yield format(command, "D1", String.valueOf(interval), "1");
            }
            case Command.TYPE_ENGINE_STOP ->
                    format(command, "S20", "1", "1");
            case Command.TYPE_ENGINE_RESUME ->
                    format(command, "S20", "0", "0");
            case Command.TYPE_ALARM_ARM ->
                    format(command, "SF", "0", "0");
            case Command.TYPE_ALARM_DISARM ->
                    format(command, "CF", "1", "1");
            default -> null;
        };
    }

}
