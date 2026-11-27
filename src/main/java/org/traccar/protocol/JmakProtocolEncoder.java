/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;

import java.util.StringJoiner;

public class JmakProtocolEncoder extends StringProtocolEncoder {

    private static final int PARAMETER_REPORT_INTERVAL = 2;
    private static final int PARAMETER_SERVER_PORT = 12;
    private static final int PARAMETER_SERVER_IP = 13;
    private static final int PARAMETER_OVERSPEED_EVENT = 16;
    private static final int PARAMETER_OVERSPEED_LIMIT = 17;
    private static final int PARAMETER_ODOMETER = 30;
    private static final int PARAMETER_OUTPUT_1 = 34;
    private static final int PARAMETER_SERVER_DOMAIN = 43;
    private static final int PARAMETER_POWER_CUT_EVENT = 55;
    private static final int PARAMETER_LOW_BATTERY_EVENT = 57;
    private static final int PARAMETER_MOVEMENT_EVENT = 59;
    private static final int PARAMETER_REBOOT = 93;
    private static final int PARAMETER_OUTPUT_2 = 94;
    private static final int PARAMETER_OUTPUT_3 = 167;

    private final boolean textChannel;

    public JmakProtocolEncoder(Protocol protocol, boolean textChannel) {
        super(protocol);
        this.textChannel = textChannel;
    }

    private String formatParameter(char action, int parameter, Object value) {
        return textChannel ? action + ";" + parameter + ";" + value : parameter + ";" + value + ";";
    }

    private String formatCommand(String... parameters) {
        StringJoiner joiner = textChannel ? new StringJoiner("#", "JMAK#", "#") : new StringJoiner("|", "$", "#");
        for (String parameter : parameters) {
            joiner.add(parameter);
        }
        return joiner.toString();
    }

    @Override
    protected Object encodeCommand(Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> command.getString(Command.KEY_DATA);
            case Command.TYPE_ENGINE_STOP -> formatCommand(formatParameter('s', PARAMETER_OUTPUT_1, 1));
            case Command.TYPE_ENGINE_RESUME -> formatCommand(formatParameter('s', PARAMETER_OUTPUT_1, 0));
            case Command.TYPE_OUTPUT_CONTROL -> {
                int parameter = switch (command.getInteger(Command.KEY_INDEX)) {
                    case 2 -> PARAMETER_OUTPUT_2;
                    case 3 -> PARAMETER_OUTPUT_3;
                    default -> PARAMETER_OUTPUT_1;
                };
                yield formatCommand(formatParameter('s', parameter, command.getString(Command.KEY_DATA)));
            }
            case Command.TYPE_POSITION_PERIODIC -> formatCommand(
                    formatParameter('s', PARAMETER_REPORT_INTERVAL, command.getString(Command.KEY_FREQUENCY)));
            case Command.TYPE_SET_ODOMETER -> formatCommand(
                    formatParameter('s', PARAMETER_ODOMETER, command.getString(Command.KEY_DATA)));
            case Command.TYPE_SET_CONNECTION -> {
                String server = command.getString(Command.KEY_SERVER);
                int parameter = server.matches("[\\d.]+") ? PARAMETER_SERVER_IP : PARAMETER_SERVER_DOMAIN;
                yield formatCommand(
                        formatParameter('s', parameter, server),
                        formatParameter('s', PARAMETER_SERVER_PORT, command.getString(Command.KEY_PORT)));
            }
            case Command.TYPE_REBOOT_DEVICE -> textChannel
                    ? "JMAK#REBOOT#" : formatCommand(formatParameter('s', PARAMETER_REBOOT, 1));
            case Command.TYPE_SET_SPEED_LIMIT -> formatCommand(
                    formatParameter('e', PARAMETER_OVERSPEED_EVENT, 1),
                    formatParameter('s', PARAMETER_OVERSPEED_LIMIT, command.getString(Command.KEY_DATA)));
            case Command.TYPE_ALARM_BATTERY -> formatCommand(
                    formatParameter('e', PARAMETER_LOW_BATTERY_EVENT, command.getBoolean(Command.KEY_ENABLE) ? 1 : 0));
            case Command.TYPE_ALARM_REMOVE -> formatCommand(
                    formatParameter('e', PARAMETER_POWER_CUT_EVENT, command.getBoolean(Command.KEY_ENABLE) ? 1 : 0));
            case Command.TYPE_ALARM_VIBRATION -> formatCommand(
                    formatParameter('e', PARAMETER_MOVEMENT_EVENT, command.getString(Command.KEY_DATA)));
            default -> null;
        };
    }

}
