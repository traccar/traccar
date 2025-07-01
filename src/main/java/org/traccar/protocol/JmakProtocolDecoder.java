/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JmakProtocolDecoder extends BaseProtocolDecoder {

    public JmakProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = ((String) msg).trim();
        if (sentence.startsWith("{") || sentence.startsWith("^")) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        Unpooled.copiedBuffer("ACK", StandardCharsets.US_ASCII),
                        remoteAddress
                ));
            }
            return null;
        }

        if (!sentence.startsWith("~") || !sentence.endsWith("$")) {
            return null;
        }
        sentence = sentence.substring(1, sentence.length() - 1);

        String[] parts = sentence.split("\\|", 2);
        String[] standard = parts[0].split(";");
        DeviceSession session = getDeviceSession(channel, remoteAddress, standard[2]);
        if (session == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(session.getDeviceId());

        int index = 0;
        int eventId = 0;
        int eventStatus = 0;
        String eventName = null;
        long mask = Long.parseLong(standard[index++], 16);

        if (BitUtil.check(mask, 0)) {
            position.set("serialNumber", standard[index++]);
        }

        if (BitUtil.check(mask, 1)) {
            position.set("imei", standard[index++]);
        }

        if (BitUtil.check(mask, 2)) {
            position.set(Position.PREFIX_COUNT, Long.parseLong(standard[index++]));
        }

        if (BitUtil.check(mask, 3)) {
            if(!standard[index].equals("NULL"))
                position.set("nickname", standard[index]);
            index++;
        }

        if (BitUtil.check(mask, 4)) {
            position.setTime(new Date(Long.parseLong(standard[index++])));
        }

        if (BitUtil.check(mask, 5)) {
            position.setLatitude(Double.parseDouble(standard[index++]));
        }

        if (BitUtil.check(mask, 6)) {
            position.setLongitude(Double.parseDouble(standard[index++]));
        }

        if (BitUtil.check(mask, 7)) {
            position.setAltitude(Double.parseDouble(standard[index++]));
        }

        if (BitUtil.check(mask, 8)) {
            index++;
        }

        if (BitUtil.check(mask, 9)) {
            double speedKnots = Double.parseDouble(standard[index++]) * 0.5399568;
            position.setSpeed(speedKnots);
        }

        if (BitUtil.check(mask, 10)) {
            position.set(Position.KEY_SATELLITES, Integer.parseInt(standard[index++]));
        }

        if (BitUtil.check(mask, 11)) {
            position.set(Position.KEY_RSSI, Integer.parseInt(standard[index++]));
        }

        if (BitUtil.check(mask, 12)) {
            position.set(Position.KEY_HDOP, Double.parseDouble(standard[index++]));
        }

        if (BitUtil.check(mask, 13)) {
            position.setCourse(Double.parseDouble(standard[index++]));
        }

        if (BitUtil.check(mask, 14)) {
            position.set(Position.KEY_IGNITION, Integer.parseInt(standard[index++]) == 1);
        }

        if (BitUtil.check(mask, 15)) {
            position.set("backup", Integer.parseInt(standard[index++]) == 1);
        }

        if (BitUtil.check(mask, 16)) {
            position.set("hourmeter", Double.parseDouble(standard[index++]));
        }

        if (BitUtil.check(mask, 17)) {
            position.set(Position.KEY_ODOMETER, Double.parseDouble(standard[index++]) * 1000);
        }

        if (BitUtil.check(mask, 18)) {
            eventId = Integer.parseInt(standard[index++]);
            position.set(Position.KEY_EVENT, eventId);
        }

        if (BitUtil.check(mask, 19)) {
            eventStatus = Integer.parseInt(standard[index++]);
            position.set("eventStatus", eventStatus);
        }

        if (BitUtil.check(mask, 20)) {
            eventName = standard[index++];
            position.set("eventName", eventName);
        }

        if (BitUtil.check(mask, 21)) {
            position.set(Position.KEY_VIN, Integer.parseInt(standard[index++]));
        }

        if (BitUtil.check(mask, 22)) {
            position.set(Position.KEY_BATTERY, Integer.parseInt(standard[index++]));
        }

        if (BitUtil.check(mask, 23)) {
            position.set(Position.KEY_OPERATOR, standard[index++]);
        }

        if (BitUtil.check(mask, 24)) {
            position.set("tec", standard[index++]);
        }

        if (BitUtil.check(mask, 25)) {
            long tsSend = Long.parseLong(standard[index++]);
            position.set("tsSend", tsSend);
        }

        if (BitUtil.check(mask, 26)) {
            position.setValid(Integer.parseInt(standard[index++]) >= 1);
        }

        if (BitUtil.check(mask, 27)) {
            int iosBits = Integer.parseInt(standard[index++]);
            position.set("input1", BitUtil.check(iosBits, 0) ? 1 : 0);
            position.set("input2", BitUtil.check(iosBits, 1) ? 1 : 0);
            position.set("output1", BitUtil.check(iosBits, 2) ? 1 : 0);
            position.set("output2", BitUtil.check(iosBits, 3) ? 1 : 0);
        }

        if (BitUtil.check(mask, 28)) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, standard[index++]);
        }

        if (BitUtil.check(mask, 29)) {
            position.set("trpSize", Integer.parseInt(standard[index++]));
        }

        if (BitUtil.check(mask, 30)) {
            position.set("trpMsg", standard[index++]);
        }

        if (BitUtil.check(mask, 31)) {
            index++;
        }

        if (BitUtil.check(mask, 32)) {
            index++;
        }

        if (BitUtil.check(mask, 33)) {
            index++;
        }

        if (BitUtil.check(mask, 34)) {
            double ain = Double.parseDouble(standard[index++]);
            position.set("ain", ain);
            position.set("discharge", ain >= 5.8 ? 1 : 0);
        }

        if (eventId == 126 && eventStatus == 4 && eventName != null) {
            position.set(Position.KEY_CARD, eventName);
        }

        if (parts.length > 1) {
            String[] can = parts[1].split(";");
            index = 0;
            mask = Long.parseLong(can[index++], 16);

            if (BitUtil.check(mask, 0)) {
                position.set(Position.KEY_OBD_ODOMETER, Double.parseDouble(can[index++]) * 1000);
            }

            if (BitUtil.check(mask, 1)) {
                position.set("canHourmeter", Double.parseDouble(can[index++]));
            }

            if (BitUtil.check(mask, 2)) {
                position.set(Position.KEY_OBD_SPEED, Double.parseDouble(can[index++]));
            }

            if (BitUtil.check(mask, 3)) {
                position.set(Position.KEY_RPM, Double.parseDouble(can[index++]));
            }

            if (BitUtil.check(mask, 4)) {
                int canOnOffBits = Integer.parseInt(can[index++]);
                position.set("canParkingBrake", BitUtil.check(canOnOffBits, 0) ? 1 : 0);
                position.set("canBrake", BitUtil.check(canOnOffBits, 1) ? 1 : 0);
                position.set("canClutch", BitUtil.check(canOnOffBits, 2) ? 1 : 0);
                position.set("canAirConditioning", BitUtil.check(canOnOffBits, 3) ? 1 : 0);
                position.set("canSeatBelt", BitUtil.check(canOnOffBits, 4) ? 1 : 0);
            }

            if (BitUtil.check(mask, 5)) {
                index++;
            }

            if (BitUtil.check(mask, 6)) {
                position.set("canPedalPressure", Double.parseDouble(can[index++]));
            }

            if (BitUtil.check(mask, 7)) {
                index++;
            }

            if (BitUtil.check(mask, 8)) {
                position.set("canFuelLevel", Double.parseDouble(can[index++]));
            }

            if (BitUtil.check(mask, 9)) {
                index++;
            }

            if (BitUtil.check(mask, 10)) {
                position.set("canAutonomy", Double.parseDouble(can[index++]));
            }

            if (BitUtil.check(mask, 11)) {
                index++;
            }

            if (BitUtil.check(mask, 12)) {
                position.set("canFuelConsumption", Double.parseDouble(can[index++]));
            }

            if (BitUtil.check(mask, 13)) {
                position.set("canFuelUsed", Double.parseDouble(can[index++]));
            }

            if (BitUtil.check(mask, 14)) {
                position.set("canOilTemperature", Double.parseDouble(can[index++]));
            }
        }

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Unpooled.copiedBuffer("ACK", StandardCharsets.US_ASCII), remoteAddress));
        }
        return position;
    }
}