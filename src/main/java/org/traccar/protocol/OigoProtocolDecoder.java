/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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

public class OigoProtocolDecoder extends BaseProtocolDecoder {

    public OigoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_AR_LOCATION = 0x00;
    public static final int MSG_AR_REMOTE_START = 0x10;

    public static final int MSG_ACKNOWLEDGEMENT = 0xE0;

    private Position decodeArMessage(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.skipBytes(1); // header
        buf.readUnsignedShort(); // length

        int type = buf.readUnsignedByte();

        int tag = buf.readUnsignedByte();

        DeviceSession deviceSession;
        switch (BitUtil.to(tag, 3)) {
            case 0:
                String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
                deviceSession = getDeviceSession(channel, remoteAddress, imei);
                break;
            case 1:
                buf.skipBytes(1);
                String meid = buf.readSlice(14).toString(StandardCharsets.US_ASCII);
                deviceSession = getDeviceSession(channel, remoteAddress, meid);
                break;
            default:
                deviceSession = getDeviceSession(channel, remoteAddress);
                break;
        }

        if (deviceSession == null || type != MSG_AR_LOCATION) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_EVENT, buf.readUnsignedByte());

        int mask = buf.readInt();

        if (BitUtil.check(mask, 0)) {
            position.set(Position.KEY_INDEX, buf.readUnsignedShort());
        }

        if (BitUtil.check(mask, 1)) {
            int date = buf.readUnsignedByte();
            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(BitUtil.between(date, 4, 8) + 2010, BitUtil.to(date, 4), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
            position.setTime(dateBuilder.getDate());
        }

        if (BitUtil.check(mask, 2)) {
            buf.skipBytes(5); // device time
        }

        if (BitUtil.check(mask, 3)) {
            position.setLatitude(buf.readUnsignedInt() * 0.000001 - 90);
            position.setLongitude(buf.readUnsignedInt() * 0.000001 - 180.0);
        }

        if (BitUtil.check(mask, 4)) {
            int status = buf.readUnsignedByte();
            position.setValid(BitUtil.between(status, 4, 8) != 0);
            position.set(Position.KEY_SATELLITES, BitUtil.to(status, 4));
            position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
        }

        if (BitUtil.check(mask, 5)) {
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        }

        if (BitUtil.check(mask, 6)) {
            position.setCourse(buf.readUnsignedShort());
        }

        if (BitUtil.check(mask, 7)) {
            position.setAltitude(buf.readShort());
        }

        if (BitUtil.check(mask, 8)) {
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 9)) {
            position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
        }

        if (BitUtil.check(mask, 10)) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
        }

        if (BitUtil.check(mask, 11)) {
            buf.skipBytes(2); // gpio
        }

        if (BitUtil.check(mask, 12)) {
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);
        }

        if (BitUtil.check(mask, 13)) {
            buf.skipBytes(6); // software version
        }

        if (BitUtil.check(mask, 14)) {
            buf.skipBytes(5); // hardware version
        }

        if (BitUtil.check(mask, 15)) {
            buf.readUnsignedShort(); // device config
        }

        return position;
    }

    private double convertCoordinate(long value) {
        boolean negative = value < 0;
        value = Math.abs(value);
        double minutes = (value % 100000) * 0.001;
        value /= 100000;
        double degrees = value + minutes / 60;
        return negative ? -degrees : degrees;
    }

    private Position decodeMgMessage(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.readUnsignedByte(); // tag
        int flags = buf.getUnsignedByte(buf.readerIndex());

        DeviceSession deviceSession;
        if (BitUtil.check(flags, 6)) {
            buf.readUnsignedByte(); // flags
            deviceSession = getDeviceSession(channel, remoteAddress);
        } else {
            String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
            deviceSession = getDeviceSession(channel, remoteAddress, imei);
        }

        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        buf.skipBytes(8); // imsi

        int date = buf.readUnsignedShort();

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(2010 + BitUtil.from(date, 12), BitUtil.between(date, 8, 12), BitUtil.to(date, 8))
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), 0);

        position.setValid(true);
        position.setLatitude(convertCoordinate(buf.readInt()));
        position.setLongitude(convertCoordinate(buf.readInt()));

        position.setAltitude(UnitsConverter.metersFromFeet(buf.readShort()));
        position.setCourse(buf.readUnsignedShort());
        position.setSpeed(UnitsConverter.knotsFromMph(buf.readUnsignedByte()));

        position.set(Position.KEY_POWER, buf.readUnsignedByte() * 0.1);
        position.set(Position.PREFIX_IO + 1, buf.readUnsignedByte());

        dateBuilder.setSecond(buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_RSSI, buf.readUnsignedByte());

        int index = buf.readUnsignedByte();

        position.set(Position.KEY_VERSION_FW, buf.readUnsignedByte());
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_ODOMETER, (long) (buf.readUnsignedInt() * 1609.34));

        if (channel != null && BitUtil.check(flags, 7)) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(MSG_ACKNOWLEDGEMENT);
            response.writeByte(index);
            response.writeByte(0x00);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getUnsignedByte(buf.readerIndex()) == 0x7e) {
            return decodeArMessage(channel, remoteAddress, buf);
        } else {
            return decodeMgMessage(channel, remoteAddress, buf);
        }
    }

}
