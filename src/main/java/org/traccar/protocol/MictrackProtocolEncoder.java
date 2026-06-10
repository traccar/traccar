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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class MictrackProtocolEncoder extends BaseProtocolEncoder {

    private static final DateTimeFormatter UTC_TIME = DateTimeFormatter
            .ofPattern("HHmmss").withZone(ZoneOffset.UTC);

    public MictrackProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private String hqFormat(Command command, String cmd, String... params) {
        String id = getUniqueId(command.getDeviceId());
        String base = String.format("*HQ,%s,%s,%s", id, cmd, UTC_TIME.format(Instant.now()));
        if (params.length > 0) {
            base += "," + String.join(",", params);
        }
        return base + "#";
    }

    private Object encodeHQCommand(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_CUSTOM ->
                    hqFormat(command, (String) command.getAttributes().get(Command.KEY_DATA));
            case Command.TYPE_POSITION_PERIODIC -> {
                int interval = ((Number) command.getAttributes().get(Command.KEY_FREQUENCY)).intValue();
                yield hqFormat(command, "D1", String.valueOf(interval), "1");
            }
            case Command.TYPE_ENGINE_STOP -> hqFormat(command, "S20", "1", "1");
            case Command.TYPE_ENGINE_RESUME -> hqFormat(command, "S20", "0", "0");
            case Command.TYPE_ALARM_ARM -> hqFormat(command, "SF", "0", "0");
            case Command.TYPE_ALARM_DISARM -> hqFormat(command, "CF", "1", "1");
            default -> null;
        };
    }

    private Object encodeMT700Command(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> {
                Object data = command.getAttributes().get(Command.KEY_DATA);
                yield data != null ? data.toString() : null;
            }
            case Command.TYPE_REBOOT_DEVICE -> "REBOOT";
            case Command.TYPE_POSITION_PERIODIC -> {
                Object freq = command.getAttributes().get(Command.KEY_FREQUENCY);
                yield freq != null ? String.format("MODE,1,%s", freq) : null;
            }
            case Command.TYPE_MODE_DEEP_SLEEP -> {
                long hours = Math.max(1, Math.min(24,
                        ((Number) command.getAttributes().get(Command.KEY_FREQUENCY)).longValue() / 3600));
                yield String.format("MODE,3,%d", hours);
            }
            case Command.TYPE_SET_CONNECTION -> {
                Object server = command.getAttributes().get(Command.KEY_SERVER);
                Object port = command.getAttributes().get(Command.KEY_PORT);
                yield server != null && port != null ? String.format("804,%s,%s", server, port) : null;
            }
            case Command.TYPE_GET_DEVICE_STATUS -> "RCONF,1";
            default -> null;
        };
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {
        String variant = channel != null ? channel.attr(MictrackProtocolDecoder.VARIANT_KEY).get() : null;
        return "hq".equals(variant) ? encodeHQCommand(command) : encodeMT700Command(command);
    }

}
