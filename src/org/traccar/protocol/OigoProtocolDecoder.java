/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class OigoProtocolDecoder extends BaseProtocolDecoder {

    public OigoProtocolDecoder(OigoProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOCATION = 0x00;
    public static final int MSG_REMOTE_START = 0x10;
    public static final int MSG_ACKNOWLEDGEMENT = 0xE0;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(1); // header
        buf.readUnsignedShort(); // length

        int type = buf.readUnsignedByte();

        int tag = buf.readUnsignedByte();

        DeviceSession deviceSession;
        switch (BitUtil.to(tag, 3)) {
            case 0:
                String imei = ChannelBuffers.hexDump(buf.readBytes(8)).substring(1);
                deviceSession = getDeviceSession(channel, remoteAddress, imei);
                break;
            case 1:
                buf.skipBytes(1);
                String meid = buf.readBytes(14).toString(StandardCharsets.US_ASCII);
                deviceSession = getDeviceSession(channel, remoteAddress, meid);
                break;
            default:
                deviceSession = getDeviceSession(channel, remoteAddress);
                break;
        }

        if (deviceSession == null || type != MSG_LOCATION) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
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
            position.set(Position.KEY_GSM, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 9)) {
            position.set(Position.KEY_POWER, buf.readUnsignedShort() + "mV");
        }

        if (BitUtil.check(mask, 10)) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() + "mV");
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

}
