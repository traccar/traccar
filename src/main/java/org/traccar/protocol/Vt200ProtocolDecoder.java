/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Date;

public class Vt200ProtocolDecoder extends BaseProtocolDecoder {

    public Vt200ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static double decodeCoordinate(int value) {
        int degrees = value / 1000000;
        int minutes = value % 1000000;
        return degrees + minutes * 0.0001 / 60;
    }

    protected Date decodeDate(ByteBuf buf) {
        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(BcdUtil.readInteger(buf, 2), BcdUtil.readInteger(buf, 2), BcdUtil.readInteger(buf, 2))
                .setTime(BcdUtil.readInteger(buf, 2), BcdUtil.readInteger(buf, 2), BcdUtil.readInteger(buf, 2));
        return dateBuilder.getDate();
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(1); // header

        String id = ByteBufUtil.hexDump(buf.readSlice(6));
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedShort();
        buf.readUnsignedShort(); // length

        if (type == 0x2086 || type == 0x2084 || type == 0x2082 || type == 0x3089) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            if (type == 0x3089) {
                position.set(Position.KEY_IGNITION, buf.readUnsignedByte() == 1);
            } else {
                buf.readUnsignedByte(); // data type
            }

            buf.readUnsignedShort(); // trip id

            position.setTime(decodeDate(buf));

            if (buf.readableBytes() > 2) {
                position.setLatitude(decodeCoordinate(BcdUtil.readInteger(buf, 8)));
                position.setLongitude(decodeCoordinate(BcdUtil.readInteger(buf, 9)));

                int flags = buf.readUnsignedByte();
                position.setValid(BitUtil.check(flags, 0));
                if (!BitUtil.check(flags, 1)) {
                    position.setLatitude(-position.getLatitude());
                }
                if (!BitUtil.check(flags, 2)) {
                    position.setLongitude(-position.getLongitude());
                }
            }

            if (type != 0x3089) {
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
                position.setCourse(buf.readUnsignedByte() * 2);

                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);
                position.set(Position.KEY_STATUS, buf.readUnsignedInt());

                // additional data
            }

            return position;

        } else if (type == 0x3088) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            buf.readUnsignedShort(); // trip id
            buf.skipBytes(8); // imei
            buf.skipBytes(8); // imsi

            position.set("tripStart", decodeDate(buf).getTime());
            position.set("tripEnd", decodeDate(buf).getTime());
            position.set("drivingTime", buf.readUnsignedShort());

            position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedInt());
            position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedInt());

            position.set("maxSpeed", UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            position.set("maxRpm", buf.readUnsignedShort());
            position.set("maxTemp", buf.readUnsignedByte() - 40);
            position.set("hardAccelerationCount", buf.readUnsignedByte());
            position.set("hardBrakingCount", buf.readUnsignedByte());

            for (String speedType : Arrays.asList("over", "high", "normal", "low")) {
                position.set(speedType + "SpeedTime", buf.readUnsignedShort());
                position.set(speedType + "SpeedDistance", buf.readUnsignedInt());
                position.set(speedType + "SpeedFuel", buf.readUnsignedInt());
            }

            position.set("idleTime", buf.readUnsignedShort());
            position.set("idleFuel", buf.readUnsignedInt());

            position.set("hardCorneringCount", buf.readUnsignedByte());
            position.set("overspeedCount", buf.readUnsignedByte());
            position.set("overheatCount", buf.readUnsignedShort());
            position.set("laneChangeCount", buf.readUnsignedByte());
            position.set("emergencyRefueling", buf.readUnsignedByte());

            return position;

        }

        return null;
    }

}
