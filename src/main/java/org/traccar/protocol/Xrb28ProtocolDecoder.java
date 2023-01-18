/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Command;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class Xrb28ProtocolDecoder extends BaseProtocolDecoder {

    private String pendingCommand;

    public void setPendingCommand(String pendingCommand) {
        this.pendingCommand = pendingCommand;
    }

    public Xrb28ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*")
            .expression("....,")
            .expression("..,")                   // vendor
            .number("d{15},")                    // imei
            .expression("..,")                   // type
            .number("[01],")                     // reserved
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss)
            .expression("([AV]),")               // validity
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d{2,3})(dd.d+),")          // longitude
            .expression("([EW]),")
            .number("(d+),")                     // satellites
            .number("(d+.d+),")                  // hdop
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(-?d+.?d*),")               // altitude
            .expression(".,")                    // height unit
            .expression(".#")                    // mode
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, sentence.substring(9, 24));
        if (deviceSession == null) {
            return null;
        }

        String type = sentence.substring(25, 27);
        if (channel != null) {
            if (type.matches("L0|L1|W0|E1")) {
                channel.write(new NetworkMessage(
                        "\u00ff\u00ff*SCOS" + sentence.substring(5, 27) + "#\n",
                        remoteAddress));
            } else if (type.equals("R0") && pendingCommand != null) {
                String command = pendingCommand.equals(Command.TYPE_ALARM_ARM) ? "L1," : "L0,";
                channel.write(new NetworkMessage(
                        "\u00ff\u00ff*SCOS" + sentence.substring(5, 25) + command + sentence.substring(30) + "\n",
                        remoteAddress));
                pendingCommand = null;
            }
        }

        if (!type.startsWith("D")) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            String payload = sentence.substring(25, sentence.length() - 1);

            int index = 0;
            String[] values = payload.substring(3).split(",");

            switch (type) {
                case "Q0":
                    position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) * 0.01);
                    position.set(Position.KEY_BATTERY_LEVEL, Integer.parseInt(values[index++]));
                    position.set(Position.KEY_RSSI, Integer.parseInt(values[index++]));
                    break;
                case "H0":
                    position.set(Position.KEY_BLOCKED, Integer.parseInt(values[index++]) > 0);
                    position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) * 0.01);
                    position.set(Position.KEY_RSSI, Integer.parseInt(values[index++]));
                    position.set(Position.KEY_BATTERY_LEVEL, Integer.parseInt(values[index++]));
                    break;
                case "W0":
                    switch (Integer.parseInt(values[index++])) {
                        case 1:
                            position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
                            break;
                        case 2:
                            position.set(Position.KEY_ALARM, Position.ALARM_FALL_DOWN);
                            break;
                        case 3:
                            position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                            break;
                        default:
                            break;
                    }
                    break;
                case "E0":
                    position.set(Position.KEY_ALARM, Position.ALARM_FAULT);
                    position.set("error", Integer.parseInt(values[index++]));
                    break;
                case "S1":
                    position.set(Position.KEY_EVENT, Integer.parseInt(values[index++]));
                    break;
                case "R0":
                case "L0":
                case "L1":
                case "S4":
                case "S5":
                case "S6":
                case "S7":
                case "V0":
                case "G0":
                case "K0":
                case "I0":
                case "M0":
                    position.set(Position.KEY_RESULT, payload);
                    break;
                default:
                    break;
            }

            return !position.getAttributes().isEmpty() ? position : null;

        } else {

            Parser parser = new Parser(PATTERN, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());

            position.set(Position.KEY_SATELLITES, parser.nextInt());
            position.set(Position.KEY_HDOP, parser.nextDouble());

            dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            position.setAltitude(parser.nextDouble());

            return position;

        }
    }

}
