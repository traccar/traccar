/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class G1rusProtocolDecoder extends BaseProtocolDecoder {
    public G1rusProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEARTBEAT = 0;
    public static final int MSG_REGULAR = 1;
    public static final int MSG_SMS_FORWARD = 2;
    public static final int MSG_SERIAL = 3;
    public static final int MSG_MIXED = 4;

    private String readString(ByteBuf buf) {
        int length = buf.readUnsignedByte() & 0xF;
        return buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
    }

    private Position decodeRegular(DeviceSession deviceSession, ByteBuf buf, int type) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(new Date((buf.readUnsignedIntLE() + 946684800) * 1000L));

        if (BitUtil.check(type, 6)) {
            position.set(Position.KEY_EVENT, buf.readUnsignedByte());
        }

        int dataMask = buf.readUnsignedShort();

        if (BitUtil.check(dataMask, 0)) {
            buf.readUnsignedByte(); // length
            readString(buf); // device name
            position.set(Position.KEY_VERSION_FW, readString(buf));
            position.set(Position.KEY_VERSION_HW, readString(buf));
        }

        if (BitUtil.check(dataMask, 1)) {
            buf.readUnsignedByte(); // length
            int locationMask = buf.readUnsignedShort();
            if (BitUtil.check(locationMask, 0)) {
                int validity = buf.readUnsignedByte();
                position.set(Position.KEY_SATELLITES, BitUtil.to(validity, 5));
                position.setValid(BitUtil.between(validity, 5, 7) == 2);
            }
            if (BitUtil.check(locationMask, 1)) {
                position.setLatitude(buf.readInt() / 1000000.0);
                position.setLongitude(buf.readInt() / 1000000.0);
            }
            if (BitUtil.check(locationMask, 2)) {
                position.setSpeed(buf.readUnsignedShort());
            }
            if (BitUtil.check(locationMask, 3)) {
                position.setCourse(buf.readUnsignedShort());
            }
            if (BitUtil.check(locationMask, 4)) {
                position.setAltitude(buf.readShort());
            }
            if (BitUtil.check(locationMask, 5)) {
                position.set(Position.KEY_HDOP, buf.readUnsignedShort());
            }
            if (BitUtil.check(locationMask, 6)) {
                position.set(Position.KEY_VDOP, buf.readUnsignedShort());
            }
        }

        if (BitUtil.check(dataMask, 2)) {
            buf.skipBytes(buf.readUnsignedByte());
        }

        if (BitUtil.check(dataMask, 3)) {
            buf.skipBytes(buf.readUnsignedByte());
        }

        if (BitUtil.check(dataMask, 4)) {
            buf.readUnsignedByte(); // length
            position.set(Position.KEY_POWER, buf.readUnsignedShort() * 110 / 4096 - 10);
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 110 / 4096 - 10);
            position.set(Position.KEY_DEVICE_TEMP, buf.readUnsignedShort() * 110 / 4096 - 10);
        }

        if (BitUtil.check(dataMask, 5)) {
            buf.skipBytes(buf.readUnsignedByte());
        }

        if (BitUtil.check(dataMask, 7)) {
            buf.skipBytes(buf.readUnsignedByte());
        }

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        buf.readUnsignedByte(); // version

        int type = buf.readUnsignedByte();
        String imei = String.valueOf(buf.readLong());
        buf.readerIndex(buf.readerIndex() - 1);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        if (BitUtil.to(type, 6) == MSG_REGULAR) {

            return decodeRegular(deviceSession, buf, type);

        } else if (BitUtil.to(type, 6) == MSG_MIXED) {

            List<Position> positions = new LinkedList<>();
            while (buf.readableBytes() > 5) {
                int length = buf.readUnsignedShort();
                int subtype = buf.readUnsignedByte();
                if (BitUtil.to(subtype, 6) == MSG_REGULAR) {
                    positions.add(decodeRegular(deviceSession, buf, subtype));
                } else {
                    buf.skipBytes(length - 1);
                }
            }
            return positions.isEmpty() ? null : positions;

        }

        buf.readUnsignedShort(); // checksum
        buf.readUnsignedByte(); // tail

        return null;

    }

}
