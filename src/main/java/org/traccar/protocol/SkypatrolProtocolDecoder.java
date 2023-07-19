/*
 * Copyright 2012 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class SkypatrolProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkypatrolProtocolDecoder.class);

    private long defaultMask;

    public SkypatrolProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected void init() {
        defaultMask = getConfig().getInteger(Keys.PROTOCOL_MASK.withPrefix(getProtocolName()));
    }

    private static double convertCoordinate(long coordinate) {
        int sign = 1;
        if (coordinate > 0x7fffffffL) {
            sign = -1;
            coordinate = 0xffffffffL - coordinate;
        }

        long degrees = coordinate / 1000000;
        double minutes = (coordinate % 1000000) / 10000.0;

        return sign * (degrees + minutes / 60);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int apiNumber = buf.readUnsignedShort();
        int commandType = buf.readUnsignedByte();
        int messageType = BitUtil.from(buf.readUnsignedByte(), 4);
        long mask = defaultMask;
        if (buf.readUnsignedByte() == 4) {
            mask = buf.readUnsignedInt();
        }

        // Binary position report
        if (apiNumber == 5 && commandType == 2 && messageType == 1 && BitUtil.check(mask, 0)) {

            Position position = new Position(getProtocolName());

            if (BitUtil.check(mask, 1)) {
                position.set(Position.KEY_STATUS, buf.readUnsignedInt());
            }

            String id;
            if (BitUtil.check(mask, 23)) {
                id = buf.toString(buf.readerIndex(), 8, StandardCharsets.US_ASCII).trim();
                buf.skipBytes(8);
            } else if (BitUtil.check(mask, 2)) {
                id = buf.toString(buf.readerIndex(), 22, StandardCharsets.US_ASCII).trim();
                buf.skipBytes(22);
            } else {
                LOGGER.warn("No device id field");
                return null;
            }
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            if (BitUtil.check(mask, 3)) {
                position.set(Position.PREFIX_IO + 1, buf.readUnsignedShort());
            }

            if (BitUtil.check(mask, 4)) {
                position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
            }

            if (BitUtil.check(mask, 5)) {
                position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
            }

            if (BitUtil.check(mask, 7)) {
                buf.readUnsignedByte(); // function category
            }

            DateBuilder dateBuilder = new DateBuilder();

            if (BitUtil.check(mask, 8)) {
                dateBuilder.setDateReverse(
                        buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
            }

            if (BitUtil.check(mask, 9)) {
                position.setValid(buf.readUnsignedByte() == 1); // gps status
            }

            if (BitUtil.check(mask, 10)) {
                position.setLatitude(convertCoordinate(buf.readUnsignedInt()));
            }

            if (BitUtil.check(mask, 11)) {
                position.setLongitude(convertCoordinate(buf.readUnsignedInt()));
            }

            if (BitUtil.check(mask, 12)) {
                position.setSpeed(buf.readUnsignedShort() / 10.0);
            }

            if (BitUtil.check(mask, 13)) {
                position.setCourse(buf.readUnsignedShort() / 10.0);
            }

            if (BitUtil.check(mask, 14)) {
                dateBuilder.setTime(
                        buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
            }

            position.setTime(dateBuilder.getDate());

            if (BitUtil.check(mask, 15)) {
                position.setAltitude(buf.readMedium());
            }

            if (BitUtil.check(mask, 16)) {
                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            }

            if (BitUtil.check(mask, 17)) {
                position.set(Position.KEY_BATTERY, buf.readUnsignedShort());
            }

            if (BitUtil.check(mask, 20)) {
                position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedInt());
            }

            if (BitUtil.check(mask, 21)) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            }

            if (BitUtil.check(mask, 22)) {
                buf.skipBytes(6); // time of message generation
            }

            if (BitUtil.check(mask, 24)) {
                position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
            }

            if (BitUtil.check(mask, 25)) {
                buf.skipBytes(18); // gps overspeed
            }

            if (BitUtil.check(mask, 26)) {
                buf.skipBytes(54); // cell information
            }

            if (BitUtil.check(mask, 28)) {
                position.set(Position.KEY_INDEX, buf.readUnsignedShort());
            }

            return position;
        }

        return null;
    }

}
