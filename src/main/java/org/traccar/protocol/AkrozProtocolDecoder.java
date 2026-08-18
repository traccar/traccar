/*
 * Copyright 2026 Mateus Azevedo (financeiro@akroztelematics.com.br)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class AkrozProtocolDecoder extends BaseProtocolDecoder {

    public AkrozProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    // Fixed width status/location block, present as the third field of every RUV message.
    private static final Pattern PATTERN_STATE = new PatternBuilder()
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("([-+]d{7})")                // latitude (x 0.00001)
            .number("([-+]d{8})")                // longitude (x 0.00001)
            .number("(ddd)")                     // speed (km/h)
            .number("(ddd)")                     // course
            .number("(d)")                       // gps fix status
            .number("xx")                        // seconds since last valid fix (hex)
            .number("(xx)")                       // digital input mask (hex)
            .number("dd")                        // reserved
            .number("(dd)")                      // hdop
            .any()                               // optional trailing data
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        String sentence = buf.toString(StandardCharsets.US_ASCII).trim();

        if (!verifyChecksum(sentence)) {
            return null;
        }

        String id = extractField(sentence, "ID=", ";");
        String index = extractField(sentence, "#", ";");
        if (id == null) {
            return null;
        }

        if (channel != null) {
            sendResponse(channel, remoteAddress, id, index);
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        String type = sentence.substring(1, 6);
        String[] values = sentence.substring(1, sentence.indexOf(";ID=")).split(",");
        if (values.length < 3 || values[0].length() < 6) {
            return null;
        }
        int event = Integer.parseInt(values[0].substring(5));

        return switch (type) {
            case "RUV01" -> decodeAkroz01(deviceSession, event, values);
            case "RUV02" -> decodeAkroz02(deviceSession, event, values);
            case "RUV03" -> decodeAkroz03(deviceSession, event, values);
            default -> null;
        };
    }

    private Position decodeAkroz01(DeviceSession deviceSession, int event, String[] values) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        decodeEvent(position, event);
        decodeLocation(position, values[2]);

        if (values.length > 16) {
            String voltage = values[3];
            position.set(Position.KEY_BATTERY, Integer.parseInt(voltage.substring(0, 4)) * 0.01);
            position.set(Position.KEY_POWER, Integer.parseInt(voltage.substring(4)) * 0.01);

            position.set(Position.KEY_HOURS, Long.parseLong(values[7]) * 60000); // minutes to milliseconds
            long odometer = Long.parseLong(values[8]);
            if (odometer > 0) {
                position.set(Position.KEY_ODOMETER, odometer);
            }
            position.set(Position.KEY_RPM, Integer.parseInt(values[9]));
            position.set(Position.KEY_COOLANT_TEMP, Integer.parseInt(values[10]));
            position.set("oilPressure", Integer.parseInt(values[11]));
            position.set(Position.KEY_FUEL_LEVEL, Integer.parseInt(values[12]));
            position.set(Position.KEY_DRIVER_UNIQUE_ID, values[16]);
        }

        return position;
    }

    private Position decodeAkroz02(DeviceSession deviceSession, int event, String[] values) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        decodeEvent(position, event);
        decodeLocation(position, values[2]);

        if (values.length > 13) {
            position.set("tripTime", Integer.parseInt(values[11])); // minutes
            position.set("tripDistance", Long.parseLong(values[12])); // meters
            position.set(Position.KEY_FUEL_USED, Long.parseLong(values[13]) / 10.0);
        }

        return position;
    }

    private Position decodeAkroz03(DeviceSession deviceSession, int event, String[] values) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        decodeEvent(position, event);
        decodeLocation(position, values[2]);

        // CAN bus parameters, present only when available on the vehicle.
        if (values.length > 22) {
            position.set(Position.KEY_THROTTLE, Integer.parseInt(values[3])); // percentage
            position.set(Position.KEY_HOURS, Long.parseLong(values[4]) * 60000); // minutes to milliseconds
            long odometer = Long.parseLong(values[5]); // meters
            if (odometer > 0) {
                position.set(Position.KEY_ODOMETER, odometer);
            }
            position.set(Position.KEY_RPM, Integer.parseInt(values[6]));
            position.set(Position.KEY_COOLANT_TEMP, Integer.parseInt(values[7])); // celsius
            position.set("oilPressure", Integer.parseInt(values[8])); // kPa
            position.set(Position.KEY_FUEL_LEVEL, Integer.parseInt(values[9])); // percentage
            position.set(Position.KEY_FUEL_USED, Long.parseLong(values[10]) / 10.0); // liters
            position.set(Position.KEY_OBD_SPEED, Integer.parseInt(values[12])); // km/h
            position.set("engineTorque", Integer.parseInt(values[13])); // percentage
            position.set("engineBrake", Integer.parseInt(values[15])); // percentage
            position.set("cruiseControl", Integer.parseInt(values[19]) == 1);
            position.set("clutchState", Integer.parseInt(values[20]) == 64);
            position.set("parkingBrake", Integer.parseInt(values[21]) == 4);
            position.set("serviceBrake", Integer.parseInt(values[22]) == 8);
        }

        return position;
    }

    private void decodeEvent(Position position, int event) {
        position.set(Position.KEY_EVENT, event);

        switch (event) {
            case 104, 106 -> position.addAlarm(Position.ALARM_OVERSPEED);
            case 109 -> position.addAlarm(Position.ALARM_HIGH_RPM);
            case 111 -> position.addAlarm(Position.ALARM_IDLE);
            case 119 -> position.addAlarm(Position.ALARM_TEMPERATURE);
            case 120 -> position.addAlarm(Position.ALARM_ACCELERATION);
            case 121 -> position.addAlarm(Position.ALARM_BRAKING);
            case 122 -> position.addAlarm(Position.ALARM_CORNERING);
            case 127 -> position.addAlarm(Position.ALARM_POWER_CUT);
            case 128 -> position.addAlarm(Position.ALARM_POWER_RESTORED);
            case 129 -> position.addAlarm(Position.ALARM_LOW_BATTERY);
            default -> {
            }
        }
    }

    private void decodeLocation(Position position, String content) {

        Parser parser = new Parser(PATTERN_STATE, content);
        if (!parser.matches()) {
            return;
        }

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLatitude(parser.nextInt() * 0.00001);
        position.setLongitude(parser.nextInt() * 0.00001);
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());

        int status = parser.nextInt();
        position.setValid(status < 8); // 8 = antenna short, 9 = no fix

        int mask = parser.nextHexInt();
        position.set(Position.KEY_IGNITION, BitUtil.check(mask, 7));
        position.set(Position.KEY_CHARGE, BitUtil.check(mask, 6));
        for (int i = 0; i < 6; i++) {
            position.set(Position.PREFIX_IN + (i + 1), !BitUtil.check(mask, i)); // active low
        }
        position.set(Position.KEY_MOTION, position.getSpeed() > 0);

        position.set(Position.KEY_HDOP, parser.nextInt());
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, String id, String index) {
        String response = ">ACK;ID=" + id + ";#" + index + ";*";
        response += String.format("%02X", checksum(response)) + "<";
        channel.writeAndFlush(new NetworkMessage(
                Unpooled.copiedBuffer(response, StandardCharsets.US_ASCII), remoteAddress));
    }

    private boolean verifyChecksum(String sentence) {
        int star = sentence.indexOf('*');
        int end = sentence.indexOf('<');
        if (star < 0 || end <= star) {
            return false;
        }
        String provided = sentence.substring(star + 1, end);
        return provided.equalsIgnoreCase(String.format("%02X", checksum(sentence.substring(0, star + 1))));
    }

    private int checksum(String data) {
        int checksum = 0;
        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) == '*' && i > 0 && data.charAt(i - 1) == ';') {
                break;
            }
            checksum ^= data.charAt(i);
        }
        return checksum & 0xFF;
    }

    private String extractField(String sentence, String prefix, String suffix) {
        int start = sentence.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = sentence.indexOf(suffix, start);
        return end < 0 ? null : sentence.substring(start, end);
    }

}
