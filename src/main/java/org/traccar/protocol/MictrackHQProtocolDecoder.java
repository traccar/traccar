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
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class MictrackHQProtocolDecoder extends BaseProtocolDecoder {

    private static final DateTimeFormatter UTC_TIME = DateTimeFormatter
            .ofPattern("HHmmss").withZone(ZoneOffset.UTC);

    public MictrackHQProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    // *HQ,ID,V1/V5/V6,hhmmss,A/V,lat,N/S,lon,E/W,speed,dir,ddmmyy,VSTATUS,MCC,MNC,LAC,CI[,extras]
    private static final Pattern PATTERN_HQ = new PatternBuilder()
            .text("*HQ,")
            .expression("([^,]+),")           // device ID
            .expression("(V\\d+),")           // data type
            .number("(dd)(dd)(dd),")          // time (hhmmss)
            .expression("([AV]),")            // validity
            .number("(d+)(dd.d+),")           // latitude (ddmm.mmmm)
            .expression("([NS]),")
            .number("(d+)(dd.d+),")           // longitude (dddmm.mmmm)
            .expression("([EW]),")
            .number("(d+.?d*),")              // speed
            .number("(d+),")                  // direction
            .number("(dd)(dd)(dd),")          // date (ddmmyy)
            .expression("([0-9A-Fa-f]{8}),")  // vehicle status (4 bytes as hex)
            .number("(d+),")                  // MCC
            .number("(d+),")                  // MNC
            .number("(d+),")                  // LAC
            .number("(d+)")                   // CI
            .any()                            // optional V5/V6 trailing fields
            .compile();

    private void decodeVehicleStatus(Position position, String statusHex) {
        // Four bytes, active-low (bit=0 means condition is active)
        int byte1 = Integer.parseInt(statusHex.substring(0, 2), 16); // device alarms
        int byte3 = Integer.parseInt(statusHex.substring(4, 6), 16); // vehicle status
        int byte4 = Integer.parseInt(statusHex.substring(6, 8), 16); // alarm status

        // Byte 1: bit1=towed, bit3=cut-off fuel/power, bit4=battery removal
        if ((byte1 & 0x02) == 0) {
            position.addAlarm(Position.ALARM_TOW);
        }
        if ((byte1 & 0x08) == 0) {
            position.addAlarm(Position.ALARM_POWER_CUT);
        }
        if ((byte1 & 0x10) == 0) {
            position.addAlarm(Position.ALARM_REMOVING);
        }

        // Byte 3: bit0=door open, bit2=ACC OFF, bit5=ACC ON
        position.set(Position.KEY_DOOR, (byte3 & 0x01) == 0);
        if ((byte3 & 0x20) == 0) {
            position.set(Position.KEY_IGNITION, true);
        } else if ((byte3 & 0x04) == 0) {
            position.set(Position.KEY_IGNITION, false);
        }

        // Byte 4: bit1=SOS, bit2=overspeed, bit3=unauthorized ignition, bit5=low battery
        if ((byte4 & 0x02) == 0) {
            position.addAlarm(Position.ALARM_SOS);
        }
        if ((byte4 & 0x04) == 0) {
            position.addAlarm(Position.ALARM_OVERSPEED);
        }
        if ((byte4 & 0x08) == 0) {
            position.addAlarm(Position.ALARM_POWER_ON);
        }
        if ((byte4 & 0x20) == 0) {
            position.addAlarm(Position.ALARM_LOW_BATTERY);
        }
    }

    // *HQ,ID,V4,firmware,YYYYMMDDHHmmss#
    private static final Pattern PATTERN_HEARTBEAT = new PatternBuilder()
            .text("*HQ,")
            .expression("([^,]+),")     // device ID
            .text("V4,")
            .expression("[^,]*,")       // firmware version
            .number("(dddd)(dd)(dd)")   // date (YYYYMMDD)
            .number("(dd)(dd)(dd)")     // time (HHmmss)
            .any()
            .compile();

    private Object decodeHeartbeat(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_HEARTBEAT, sentence);
        if (!parser.matches()) {
            return null;
        }
        String deviceId = parser.next();
        if (getDeviceSession(channel, remoteAddress, deviceId) == null) {
            return null;
        }
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    String.format("HQ,%s,R12,%s#", deviceId, UTC_TIME.format(Instant.now())),
                    remoteAddress));
        }
        return null;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = ((String) msg).trim();

        if (!sentence.startsWith("*HQ,")) {
            return null;
        }

        if (sentence.contains(",V4,")) {
            return decodeHeartbeat(channel, remoteAddress, sentence);
        }

        Parser parser = new Parser(PATTERN_HQ, sentence);
        if (!parser.matches()) {
            return null;
        }

        String deviceId = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            return null;
        }

        // Send R12 acknowledgement
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    String.format("HQ,%s,R12,%s#", deviceId, UTC_TIME.format(Instant.now())),
                    remoteAddress));
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String dataType = parser.next();

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        decodeVehicleStatus(position, parser.next());

        int mcc = parser.nextInt();
        int mnc = parser.nextInt();
        int lac = parser.nextInt();
        int cid = parser.nextInt();
        position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid)));

        // V5: trailing mileage,voltage
        if (dataType.equals("V5")) {
            String remaining = sentence.substring(sentence.lastIndexOf(String.valueOf(cid)) + String.valueOf(cid).length());
            String[] extras = remaining.replaceAll("[,#]", ",").split(",");
            int idx = 0;
            while (idx < extras.length && extras[idx].isEmpty()) {
                idx++;
            }
            if (idx < extras.length) {
                try {
                    position.set(Position.KEY_ODOMETER, Long.parseLong(extras[idx].trim()) * 1000);
                } catch (NumberFormatException e) {
                    // ignore
                }
                idx++;
            }
            if (idx < extras.length) {
                try {
                    int voltRaw = Integer.parseInt(extras[idx].trim());
                    position.set(Position.KEY_POWER, voltRaw / 10.0);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        return position;
    }

}
