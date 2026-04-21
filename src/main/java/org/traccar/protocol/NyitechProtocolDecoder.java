/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.ObdDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class NyitechProtocolDecoder extends BaseProtocolDecoder {

    public NyitechProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final short MSG_LOGIN = 0x1001;
    public static final short MSG_COMPREHENSIVE_LIVE = 0x2001;
    public static final short MSG_COMPREHENSIVE_HISTORY = 0x2002;
    public static final short MSG_ALARM = 0x2003;
    public static final short MSG_FIXED = 0x2004;

    private void decodeLocation(Position position, ByteBuf buf) {

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        int flags = buf.readUnsignedByte();
        position.setValid(BitUtil.to(flags, 2) > 0);

        double lat = buf.readUnsignedIntLE() / 3600000.0;
        double lon = buf.readUnsignedIntLE() / 3600000.0;

        position.setLatitude(BitUtil.check(flags, 2) ? lat : -lat);
        position.setLongitude(BitUtil.check(flags, 3) ? lon : -lon);

        position.setSpeed(UnitsConverter.knotsFromCps(buf.readUnsignedShortLE()));
        position.setCourse(buf.readUnsignedShortLE() * 0.1);
        position.setAltitude(buf.readShortLE() * 0.1);
    }

    private String decodeAlarm(int type) {
        return switch (type) {
            case 0x09 -> Position.ALARM_ACCELERATION;
            case 0x0a -> Position.ALARM_BRAKING;
            case 0x0b -> Position.ALARM_CORNERING;
            case 0x0e -> Position.ALARM_SOS;
            default -> null;
        };
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShortLE(); // length

        String id = buf.readCharSequence(12, StandardCharsets.US_ASCII).toString();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedShortLE();

        if (type != MSG_LOGIN && type != MSG_COMPREHENSIVE_LIVE
                && type != MSG_COMPREHENSIVE_HISTORY && type != MSG_ALARM && type != MSG_FIXED) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (type == MSG_COMPREHENSIVE_LIVE || type == MSG_COMPREHENSIVE_HISTORY) {

            buf.skipBytes(6); // time
            boolean includeLocation = buf.readUnsignedByte() > 0;
            boolean includeObd = buf.readUnsignedByte() > 0;
            buf.readUnsignedByte(); // include sensor

            if (includeLocation) {
                decodeLocation(position, buf);
            } else {
                getLastLocation(position, null);
            }

            if (includeObd) {
                int count = buf.readUnsignedByte();
                for (int i = 0; i < count; i++) {
                    int pid = buf.readUnsignedShortLE();
                    int length = buf.readUnsignedByte();
                    switch (length) {
                        case 1 -> position.add(ObdDecoder.decodeData(pid, buf.readByte(), true));
                        case 2 -> position.add(ObdDecoder.decodeData(pid, buf.readShortLE(), true));
                        case 4 -> position.add(ObdDecoder.decodeData(pid, buf.readIntLE(), true));
                        default -> buf.skipBytes(length);
                    }
                }
            }

            position.set(Position.KEY_FUEL_USED, buf.readUnsignedInt() * 0.01);
            position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedInt());


        } else if (type == MSG_ALARM) {

            buf.readUnsignedShortLE(); // random number
            buf.readUnsignedByte(); // tag
            position.addAlarm(decodeAlarm(buf.readUnsignedByte()));
            buf.readUnsignedShortLE(); // threshold
            buf.readUnsignedShortLE(); // value
            buf.skipBytes(6); // time

            decodeLocation(position, buf);

        } else if (type == MSG_FIXED) {

            buf.skipBytes(6); // time

            decodeLocation(position, buf);

        } else {

            decodeLocation(position, buf);

        }

        return position;
    }

}
