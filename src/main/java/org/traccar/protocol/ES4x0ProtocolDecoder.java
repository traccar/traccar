/*
 * Copyright 2024 - 2025 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.session.DeviceSession;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class ES4x0ProtocolDecoder extends BaseProtocolDecoder {

    private static final byte MESSAGE_TYPE_REGULAR = 0x52; // R
    private static final byte MESSAGE_TYPE_MAINTENANCE = 0x4D; // M
    private static final byte MESSAGE_TYPE_OBD = 0x4F; // O

    private static final int EVENT_ID_AUTO_REPORT = 0;
    private static final int EVENT_ID_POWER_CUT = 2;
    private static final int EVENT_ID_POWER_RECOVER = 3;
    private static final int EVENT_ID_IGNITION_ON = 4;
    private static final int EVENT_ID_IGNITION_OFF = 5;
    private static final int EVENT_ID_STOP = 8;
    private static final int EVENT_ID_HEARTBEAT = 9;
    private static final int EVENT_ID_LOW_CAR_BATTERY = 11;
    private static final int EVENT_ID_NOMOVE = 12;
    private static final int EVENT_ID_INPUT1_HIGH = 13;
    private static final int EVENT_ID_INPUT1_LOW = 14;
    private static final int EVENT_ID_TRIP_START = 15;
    private static final int EVENT_ID_TRIP_END = 16;
    private static final int EVENT_ID_LOCATE_NOW = 20;
    private static final int EVENT_ID_OVER_SPEED = 21;
    private static final int EVENT_ID_LOW_BACKUP_BATTERY = 23;
    private static final int EVENT_ID_HEADING_CHANGE = 24;
    private static final int EVENT_ID_TOW = 25;
    private static final int EVENT_ID_HARD_BREAK = 32;
    private static final int EVENT_ID_HARSH_ACCEL = 33;
    private static final int EVENT_ID_CAR_CRASH = 34;
    private static final int EVENT_ID_OUTPUT_SET = 35;
    private static final int EVENT_ID_OUTPUT_CLEAR = 36;
    private static final int EVENT_ID_HARD_TURN = 37;

    private static final String[] EVENT_NAMES = {
        "Auto Report", null, "Power Cut", "Power Recover", "Ignition On", "Ignition Off",
        null, null, "Stop", "Heart Beat", "Stolen Mode", "Low Car Battery", "Nomove",
        "Input 1 High", "Input 1 Low", "Trip Start", "Trip End", null, null, null,
        "Locate Now", "Over Speed", null, "Low Backup Battery", "Heading Change", "Tow",
        null, null, null, null, null, null, "Hard Break", "Harsh Accel", "Car Crash",
        "Output Set", "Output Clear", "Hard Turn"
    };

    public ES4x0ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        
        buf.skipBytes(5); // Skip header "ET410"
        
        String imei = decodeImei(buf);
        
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        byte messageType = buf.readByte();
        buf.readUnsignedByte(); // Skip sequence number
        
        long messageTime = buf.readUnsignedInt();
        Date deviceTime = new Date(messageTime * 1000);

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(deviceTime);
        position.setServerTime(new Date());

        switch (messageType) {
            case MESSAGE_TYPE_REGULAR:
                decodeRegularReport(position, buf);
                break;
            case MESSAGE_TYPE_MAINTENANCE:
                decodeMaintenanceReport(position, buf);
                break;
            case MESSAGE_TYPE_OBD:
                decodeObdReport(position, buf);
                break;
            default:
                return null;
        }

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(buf.retain(), remoteAddress));
        }

        return position;
    }

    private void decodeRegularReport(Position position, ByteBuf buf) {
        int payloadMask = buf.readUnsignedShort();
        
        if (BitUtil.check(payloadMask, 0)) {
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }
        if (BitUtil.check(payloadMask, 1)) {
            position.set(Position.KEY_HDOP, buf.readUnsignedByte() / 10.0);
        }
        
        if (BitUtil.check(payloadMask, 2)) {
            long gpsTime = buf.readUnsignedInt();
            position.setDeviceTime(new Date(gpsTime * 1000));
            position.setFixTime(new Date(gpsTime * 1000));
        }
        
        if (BitUtil.check(payloadMask, 3)) {
            int lat = buf.readInt();
            position.setLatitude(lat / 10000000.0);
        }
        if (BitUtil.check(payloadMask, 4)) {
            int lon = buf.readInt();
            position.setLongitude(lon / 10000000.0);
        }
        position.setValid(position.getLatitude() != 0 || position.getLongitude() != 0);
        
        if (BitUtil.check(payloadMask, 5)) {
            int speedCm = buf.readInt();
            position.setSpeed(speedCm / 100.0 * 3.6);
        }
        
        if (BitUtil.check(payloadMask, 6)) {
            position.setCourse(buf.readUnsignedShort());
        }
        
        if (BitUtil.check(payloadMask, 7)) {
            int inputStatus = buf.readUnsignedByte();
            position.set(Position.KEY_INPUT, inputStatus);
            position.set(Position.KEY_IGNITION, BitUtil.check(inputStatus, 0));
        }
        
        if (BitUtil.check(payloadMask, 8)) {
            position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
        }
        
        if (BitUtil.check(payloadMask, 9)) {
            int eventId = buf.readUnsignedByte();
            position.set(Position.KEY_EVENT, eventId);
            String alarm = decodeEventId(eventId);
            if (alarm != null) {
                position.addAlarm(alarm);
            }
        }
        
        if (BitUtil.check(payloadMask, 10)) {
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
        }
        
        if (BitUtil.check(payloadMask, 11)) {
            position.set("adc", buf.readUnsignedShort() / 100.0);
        }
        
        if (BitUtil.check(payloadMask, 12)) {
            position.set(Position.KEY_POWER, buf.readUnsignedShort() / 100.0);
        }
        
        if (BitUtil.check(payloadMask, 13)) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
        }
        
        if (BitUtil.check(payloadMask, 14)) {
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        }
    }

    private void decodeMaintenanceReport(Position position, ByteBuf buf) {
        int payloadMask = buf.readUnsignedShort();

        if (BitUtil.check(payloadMask, 0)) {
            int eventId = buf.readUnsignedByte();
            position.set(Position.KEY_EVENT, eventId);
            String alarm = decodeEventId(eventId);
            if (alarm != null) {
                position.addAlarm(alarm);
            }
        }
        if (BitUtil.check(payloadMask, 1)) {
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        }
        if (BitUtil.check(payloadMask, 2)) {
            buf.readUnsignedByte(); // Network Registration
        }
        if (BitUtil.check(payloadMask, 3)) {
            buf.readUnsignedByte(); // PDP State
        }
        if (BitUtil.check(payloadMask, 4)) {
            buf.readUnsignedByte(); // Message Queue
        }
        if (BitUtil.check(payloadMask, 5)) {
            byte[] iccidBytes = new byte[20];
            buf.readBytes(iccidBytes);
            String iccid = new String(iccidBytes, StandardCharsets.US_ASCII).trim();
            position.set(Position.KEY_ICCID, iccid);
        }
        if (BitUtil.check(payloadMask, 6)) {
            byte[] fwBytes = new byte[35];
            buf.readBytes(fwBytes);
            String fwVersion = new String(fwBytes, StandardCharsets.US_ASCII).trim();
            position.set(Position.KEY_VERSION_FW, fwVersion);
        }
        if (BitUtil.check(payloadMask, 7)) {
            byte[] hwBytes = new byte[14];
            buf.readBytes(hwBytes);
            String hwVersion = new String(hwBytes, StandardCharsets.US_ASCII).trim();
            position.set(Position.KEY_VERSION_HW, hwVersion);
        }

        getLastLocation(position, null);
    }

    private void decodeObdReport(Position position, ByteBuf buf) {
        int payloadMask = buf.readUnsignedShort();

        if (BitUtil.check(payloadMask, 0)) {
            int eventId = buf.readUnsignedByte();
            position.set(Position.KEY_EVENT, eventId);
            String alarm = decodeEventId(eventId);
            if (alarm != null) {
                position.addAlarm(alarm);
            }
        }
        if (BitUtil.check(payloadMask, 1)) {
            byte[] vinBytes = new byte[17];
            buf.readBytes(vinBytes);
            String vin = new String(vinBytes, StandardCharsets.US_ASCII).trim();
            position.set(Position.KEY_VIN, vin);
        }
        if (BitUtil.check(payloadMask, 2)) {
            position.set(Position.KEY_RPM, (double) buf.readUnsignedShort());
        }
        if (BitUtil.check(payloadMask, 3)) {
            position.set(Position.KEY_OBD_SPEED, buf.readUnsignedByte());
        }
        if (BitUtil.check(payloadMask, 4)) {
            position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte());
        }
        if (BitUtil.check(payloadMask, 5)) {
            position.set("milState", buf.readUnsignedByte());
        }
        if (BitUtil.check(payloadMask, 6)) {
            position.set(Position.KEY_IGNITION, buf.readUnsignedByte() == 1);
        }
        if (BitUtil.check(payloadMask, 7)) {
            int dtcCount = buf.readUnsignedByte();
            if (dtcCount > 0) {
                StringBuilder dtcs = new StringBuilder();
                for (int i = 0; i < dtcCount; i++) {
                    byte[] dtcBytes = new byte[5];
                    buf.readBytes(dtcBytes);
                    String dtc = new String(dtcBytes, StandardCharsets.US_ASCII).trim();
                    if (i > 0) dtcs.append(",");
                    dtcs.append(dtc);
                }
                position.set(Position.KEY_DTCS, dtcs.toString());
            }
        }

        getLastLocation(position, null);
    }

    private String decodeEventId(int eventId) {
        if (eventId >= 0 && eventId < EVENT_NAMES.length && EVENT_NAMES[eventId] != null) {
            return EVENT_NAMES[eventId].toLowerCase().replace(' ', '_');
        }
        return switch (eventId) {
            case EVENT_ID_IGNITION_ON -> Position.ALARM_GENERAL;
            case EVENT_ID_OVER_SPEED -> Position.ALARM_OVERSPEED;
            case EVENT_ID_LOW_CAR_BATTERY, EVENT_ID_LOW_BACKUP_BATTERY -> Position.ALARM_LOW_BATTERY;
            case EVENT_ID_POWER_CUT -> Position.ALARM_POWER_CUT;
            case EVENT_ID_HARD_BREAK -> Position.ALARM_BRAKING;
            case EVENT_ID_HARSH_ACCEL -> Position.ALARM_ACCELERATION;
            case EVENT_ID_CAR_CRASH -> Position.ALARM_ACCIDENT;
            case EVENT_ID_TOW -> Position.ALARM_TOW;
            default -> null;
        };
    }

    private String decodeImei(ByteBuf buf) {
        StringBuilder imei = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            byte b = buf.readByte();
            int hi = (b >> 4) & 0x0F;
            int lo = b & 0x0F;
            if (hi <= 9) imei.append(hi);
            if (lo <= 9 && lo != 0xF) imei.append(lo);
        }
        return imei.toString();
    }

}