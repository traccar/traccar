/*
 * Copyright 2016 - 2024 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class AstraProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AstraProtocolDecoder.class);

    public AstraProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEARTBEAT = 0x1A;
    public static final int MSG_DATA = 0x10;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        byte protocol = buf.readByte();
        buf.readUnsignedShort(); // length

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(Unpooled.wrappedBuffer(new byte[] {0x06}), remoteAddress));
        }

        return switch (protocol) {
            case 'K' -> decodeK(channel, remoteAddress, buf);
            case 'X' -> decodeX(channel, remoteAddress, buf);
            default -> null;
        };
    }

    private String readImei(ByteBuf buf) {
        return String.format("%08d", buf.readUnsignedInt()) + String.format("%07d", buf.readUnsignedMedium());
    }

    private Date readTime(ByteBuf buf) {
        DateBuilder dateBuilder = new DateBuilder()
                .setDate(1980, 1, 6).addMillis(buf.readUnsignedInt() * 1000L);
        return dateBuilder.getDate();
    }

    private Object decodeK(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, readImei(buf));
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();

        while (buf.readableBytes() > 2) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            buf.readUnsignedByte(); // index

            position.setValid(true);
            position.setLatitude(buf.readInt() * 0.000001);
            position.setLongitude(buf.readInt() * 0.000001);
            position.setTime(readTime(buf));
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte() * 2));
            position.setCourse(buf.readUnsignedByte() * 2);

            int reason = buf.readUnsignedMedium();
            position.set(Position.KEY_EVENT, reason);

            int status = buf.readUnsignedShort();
            position.set(Position.KEY_STATUS, status);

            position.set(Position.PREFIX_IO + 1, buf.readUnsignedByte());
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedByte());
            position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
            position.set(Position.KEY_POWER, buf.readUnsignedByte());

            buf.readUnsignedByte(); // max journey speed
            buf.skipBytes(6); // accelerometer
            position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedShort());
            buf.readUnsignedShort(); // journey idle time

            position.setAltitude(buf.readUnsignedByte() * 20);

            int quality = buf.readUnsignedByte();
            position.set(Position.KEY_SATELLITES, quality & 0xf);
            position.set(Position.KEY_RSSI, quality >> 4);

            buf.readUnsignedByte(); // geofence events

            if (BitUtil.check(status, 8)) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, buf.readSlice(7).toString(StandardCharsets.US_ASCII));
                position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium() * 1000);
                position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(buf.readUnsignedShort()));
            }

            if (BitUtil.check(status, 6)) {
                LOGGER.warn("Extension data is not supported");
                return position;
            }

            positions.add(position);

        }

        return positions;
    }

    private Object decodeX(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        int count = buf.readUnsignedByte();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, readImei(buf));
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();
        for (int i = 0; i < count; i++) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_INDEX, buf.readUnsignedByte());
            long mask = ((long) buf.readUnsignedShort() << 32) + buf.readUnsignedInt();
            position.setDeviceTime(readTime(buf));
            position.set(Position.KEY_EVENT, buf.readUnsignedInt());
            position.set(Position.KEY_STATUS, buf.readUnsignedShort());

            if ((mask & 1L) > 0) {
                position.set(Position.KEY_POWER, buf.readUnsignedByte() * 0.2);
                position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
            }

            if ((mask & 2L) > 0) {
                position.setValid(true);
                position.setFixTime(readTime(buf));
                position.setLatitude(buf.readInt() * 0.000001);
                position.setLongitude(buf.readInt() * 0.000001);
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte() * 2));
                buf.readUnsignedByte(); // max speed since last report
                position.setCourse(buf.readUnsignedByte() * 2);
                position.setAltitude(buf.readUnsignedByte() * 20);
                position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedShort() * 100);
            } else {
                getLastLocation(position, position.getDeviceTime());
            }

            if ((mask & 4L) > 0) {
                buf.readUnsignedShort(); // states
                buf.readUnsignedShort(); // changes mask
            }

            if ((mask & 8L) > 0) {
                buf.readUnsignedShort(); // adc1
                buf.readUnsignedShort(); // adc2
            }

            if ((mask & 16L) > 0) {
                position.set("xMax", buf.readByte());
                position.set("xMin", buf.readByte());
                position.set("yMax", buf.readByte());
                position.set("yMin", buf.readByte());
                position.set("zMax", buf.readByte());
                position.set("zMin", buf.readByte());
                position.set("idleHours", buf.readUnsignedShort());
            }

            if ((mask & 32L) > 0) {
                int value = buf.readUnsignedByte();
                position.set(Position.KEY_SATELLITES, BitUtil.to(value, 4));
                position.set(Position.KEY_RSSI, BitUtil.from(value, 4));
            }

            if ((mask & 64L) > 0) {
                buf.readUnsignedShort(); // mcc
                buf.readUnsignedShort(); // mnc
            }

            if ((mask & 128L) > 0) {
                buf.readUnsignedByte(); // geofences
            }

            if ((mask & 256L) > 0) {
                buf.readUnsignedByte(); // source
                buf.readLong(); // driver id
            }

            if ((mask & 512L) > 0) {
                buf.readUnsignedByte(); // source
                buf.skipBytes(10); // trailer id
                buf.readUnsignedByte(); // status
            }

            if ((mask & 1024L) > 0) {
                position.set("axleWeight", buf.readUnsignedShort());
            }

            if ((mask & 2048L) > 0) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium() * 1000);
                position.set(Position.KEY_HOURS, buf.readUnsignedShort() * 3_600_000);
            }

            if ((mask & 4096L) > 0) {
                position.set("wheelSpeedMax", buf.readUnsignedByte());
                position.set("wheelSpeedAvg", buf.readUnsignedByte());
                position.set("rpmMax", buf.readUnsignedByte() * 32);
                position.set("rpmAvg", buf.readUnsignedByte() * 32);
                position.set("acceleratorMax", buf.readUnsignedByte());
                position.set("acceleratorAvg", buf.readUnsignedByte());
                position.set("engineLoadMax", buf.readUnsignedByte());
                position.set("engineLoadAvg", buf.readUnsignedByte());
                position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedShort() * 100);
                position.set(Position.KEY_COOLANT_TEMP, buf.readByte() + 40);
                position.set("fmsStatus", buf.readUnsignedShort());
                position.set("fmsEvents", buf.readUnsignedShort());
                position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte());
                position.set(Position.KEY_FUEL_USED, buf.readUnsignedInt() * 0.5);
            }

            if ((mask & 8192L) > 0) {
                position.set("wheelSpeedMax", buf.readUnsignedByte());
                position.set("wheelSpeedAvg", buf.readUnsignedByte());
                position.set("rpmMax", buf.readUnsignedByte() * 32);
                position.set("rpmAvg", buf.readUnsignedByte() * 32);
                position.set("acceleratorMax", buf.readUnsignedByte());
                position.set("acceleratorAvg", buf.readUnsignedByte());
                position.set("engineLoadMax", buf.readUnsignedByte());
                position.set("engineLoadAvg", buf.readUnsignedByte());
                position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedShort() * 100);
                position.set(Position.KEY_COOLANT_TEMP, buf.readByte() + 40);
                position.set("obdStatus", buf.readUnsignedShort());
                position.set("obdEvents", buf.readUnsignedShort());
                position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte());
                position.set(Position.KEY_FUEL_USED, buf.readUnsignedShort() * 0.1);
            }

            if ((mask & 16384L) > 0) {
                for (int j = 1; j <= 5; j++) {
                    position.set("dtc" + j, buf.readCharSequence(5, StandardCharsets.US_ASCII).toString());
                }
            }

            if ((mask & 32768L) > 0) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium() * 1000);
                position.set(Position.KEY_HOURS, buf.readUnsignedShort() * 3_600_000);
                position.set("axleWeight", buf.readUnsignedShort());
                position.set("tripFuelUsed", buf.readUnsignedShort() * 0.1);
                position.set("tripCruise", buf.readUnsignedShort());
                position.set(Position.KEY_ODOMETER_SERVICE, buf.readUnsignedShort() * 5);
            }

            if ((mask & 65536L) > 0) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium() * 1000);
                position.set(Position.KEY_HOURS, buf.readUnsignedShort() * 3_600_000);
                buf.readUnsignedShort(); // time with mil on
                buf.readUnsignedShort(); // distance with mil on
            }

            if ((mask & 131072L) > 0) {
                for (int j = 1; j <= 6; j++) {
                    position.set(Position.PREFIX_TEMP + j, buf.readShort() * 0.1);
                }
                for (int j = 1; j <= 3; j++) {
                    position.set("setpoint" + j, buf.readByte() * 0.5);
                }
                buf.readUnsignedByte(); // refrigerator fuel level
                buf.readUnsignedShort(); // refrigerator total engine hours
                buf.readUnsignedShort(); // refrigerator total standby hours
                buf.readUnsignedShort(); // refrigerator status
                buf.readUnsignedMedium(); // alarm flags
            }

            if ((mask & 262144L) > 0) {
                for (int j = 1; j <= 4; j++) {
                    position.set(Position.PREFIX_TEMP + j, (buf.readUnsignedShort() - 550) * 0.1);
                }
            }

            if ((mask & 524288L) > 0) {
                position.set("alarmCount", buf.readUnsignedByte());
                position.set("alarmQueue", ByteBufUtil.hexDump(buf.readSlice(16)));
            }

            if ((mask & 4294967296L) > 0) {
                for (int j = 1; j <= 6; j++) {
                    position.set("sensor" + j, buf.readUnsignedMedium());
                }
            }

            positions.add(position);

        }

        return positions;
    }

}
