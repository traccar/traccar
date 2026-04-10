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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Es4x0ProtocolDecoder extends BaseProtocolDecoder {

    public Es4x0ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_REGULAR = 0x52;
    public static final int MSG_MAINTENANCE = 0x4D;
    public static final int MSG_OBD = 0x4F;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(5); // header

        StringBuilder imei = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            byte b = buf.readByte();
            int hi = (b >> 4) & 0x0F;
            int lo = b & 0x0F;
            if (hi <= 9) {
                imei.append(hi);
            }
            if (lo <= 9 && lo != 0xF) {
                imei.append(lo);
            }
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei.toString());
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedByte();
        buf.readUnsignedByte(); // index

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(new Date(buf.readUnsignedInt() * 1000));

        switch (type) {
            case MSG_REGULAR:
                decodeRegular(position, buf);
                break;
            case MSG_MAINTENANCE:
                decodeMaintenance(position, buf);
                break;
            case MSG_OBD:
                decodeObd(position, buf);
                break;
            default:
                return null;
        }

        return position;
    }

    private void decodeRegular(Position position, ByteBuf buf) {

        int mask = buf.readUnsignedShort();
        boolean valid = false;

        if (BitUtil.check(mask, 0)) {
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }
        if (BitUtil.check(mask, 1)) {
            position.set(Position.KEY_HDOP, buf.readUnsignedByte() / 10.0);
        }
        if (BitUtil.check(mask, 2)) {
            position.setTime(new Date(buf.readUnsignedInt() * 1000));
        }
        if (BitUtil.check(mask, 3)) {
            position.setLatitude(buf.readInt() / 10000000.0);
            valid = true;
        }
        if (BitUtil.check(mask, 4)) {
            position.setLongitude(buf.readInt() / 10000000.0);
            valid = true;
        }
        if (BitUtil.check(mask, 5)) {
            position.setSpeed(UnitsConverter.kphFromKnots(UnitsConverter.knotsFromCps(buf.readInt())));
        }
        if (BitUtil.check(mask, 6)) {
            position.setCourse(buf.readUnsignedShort());
        }
        if (BitUtil.check(mask, 7)) {
            int input = buf.readUnsignedByte();
            position.set(Position.KEY_INPUT, input);
            position.set(Position.KEY_IGNITION, BitUtil.check(input, 0));
        }
        if (BitUtil.check(mask, 8)) {
            position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
        }
        if (BitUtil.check(mask, 9)) {
            int event = buf.readUnsignedByte();
            position.set(Position.KEY_EVENT, event);
            position.addAlarm(decodeAlarm(event));
        }
        if (BitUtil.check(mask, 10)) {
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
        }
        if (BitUtil.check(mask, 11)) {
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() / 100.0);
        }
        if (BitUtil.check(mask, 12)) {
            position.set(Position.KEY_POWER, buf.readUnsignedShort() / 100.0);
        }
        if (BitUtil.check(mask, 13)) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
        }
        if (BitUtil.check(mask, 14)) {
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        }

        position.setValid(valid);
    }

    private void decodeMaintenance(Position position, ByteBuf buf) {

        int mask = buf.readUnsignedShort();

        if (BitUtil.check(mask, 0)) {
            int event = buf.readUnsignedByte();
            position.set(Position.KEY_EVENT, event);
            position.addAlarm(decodeAlarm(event));
        }
        if (BitUtil.check(mask, 1)) {
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        }
        if (BitUtil.check(mask, 2)) {
            buf.readUnsignedByte(); // network registration
        }
        if (BitUtil.check(mask, 3)) {
            buf.readUnsignedByte(); // pdp state
        }
        if (BitUtil.check(mask, 4)) {
            buf.readUnsignedByte(); // message queue
        }
        if (BitUtil.check(mask, 5)) {
           position.set(Position.KEY_ICCID, buf.readCharSequence(20, StandardCharsets.US_ASCII).toString());
        }
        if (BitUtil.check(mask, 6)) {
             position.set(Position.KEY_VERSION_FW, buf.readCharSequence(35, StandardCharsets.US_ASCII).toString());
        }
        if (BitUtil.check(mask, 7)) {
            position.set(Position.KEY_VERSION_HW, buf.readCharSequence(14, StandardCharsets.US_ASCII).toString());
        }

        getLastLocation(position, null);
    }

    private void decodeObd(Position position, ByteBuf buf) {

        int mask = buf.readUnsignedShort();

        if (BitUtil.check(mask, 0)) {
            int event = buf.readUnsignedByte();
            position.set(Position.KEY_EVENT, event);
            position.addAlarm(decodeAlarm(event));
        }
        if (BitUtil.check(mask, 1)) {
            position.set(Position.KEY_VIN, buf.readCharSequence(17, StandardCharsets.US_ASCII).toString());
        }
        if (BitUtil.check(mask, 2)) {
            position.set(Position.KEY_RPM, buf.readUnsignedShort());
        }
        if (BitUtil.check(mask, 3)) {
            position.set(Position.KEY_OBD_SPEED, buf.readUnsignedByte());
        }
        if (BitUtil.check(mask, 4)) {
            position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte());
        }
        if (BitUtil.check(mask, 5)) {
            position.set("milState", buf.readUnsignedByte());
        }
        if (BitUtil.check(mask, 6)) {
            position.set(Position.KEY_IGNITION, buf.readUnsignedByte() == 1);
        }
        if (BitUtil.check(mask, 7)) {
            int dtcCount = buf.readUnsignedByte();
            if (dtcCount > 0) {
                StringBuilder dtcs = new StringBuilder();
                for (int i = 0; i < dtcCount; i++) {
                    String dtc = buf.readCharSequence(5, StandardCharsets.US_ASCII).toString();
                    if (i > 0) {
                        dtcs.append(",");
                    }
                    dtcs.append(dtc);
                }
                position.set(Position.KEY_DTCS, dtcs.toString());
            }
        }

        getLastLocation(position, null);
    }

    private String decodeAlarm(int event) {
        return switch (event) {
            case 0x04 -> Position.ALARM_GENERAL;
            case 0x15 -> Position.ALARM_OVERSPEED;
            case 0x0B -> Position.ALARM_LOW_BATTERY;
            case 0x17 -> Position.ALARM_LOW_POWER;
            case 0x02 -> Position.ALARM_POWER_CUT;
            case 0x20 -> Position.ALARM_BRAKING;
            case 0x21 -> Position.ALARM_ACCELERATION;
            case 0x22 -> Position.ALARM_ACCIDENT;
            case 0x19 -> Position.ALARM_TOW;
            default -> null;
        };
    }

}
