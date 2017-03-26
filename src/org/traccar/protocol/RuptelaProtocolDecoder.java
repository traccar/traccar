/*
 * Copyright 2013 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import javax.xml.bind.DatatypeConverter;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RuptelaProtocolDecoder extends BaseProtocolDecoder {

    public RuptelaProtocolDecoder(RuptelaProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_RECORDS = 1;
    public static final int MSG_DEVICE_CONFIGURATION = 2;
    public static final int MSG_DEVICE_VERSION = 3;
    public static final int MSG_FIRMWARE_UPDATE = 4;
    public static final int MSG_SET_CONNECTION = 5;
    public static final int MSG_SET_ODOMETER = 6;
    public static final int MSG_SMS_VIA_GPRS_RESPONSE = 7;
    public static final int MSG_SMS_VIA_GPRS = 8;
    public static final int MSG_DTCS = 9;
    public static final int MSG_SET_IO = 17;
    public static final int MSG_EXTENDED_RECORDS = 68;

    private Position decodeCommandResponse(DeviceSession deviceSession, int type, ChannelBuffer buf) {
        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_TYPE, type);

        switch (type) {
            case MSG_DEVICE_CONFIGURATION:
            case MSG_DEVICE_VERSION:
            case MSG_FIRMWARE_UPDATE:
            case MSG_SMS_VIA_GPRS_RESPONSE:
                position.set(Position.KEY_RESULT,
                        buf.toString(buf.readerIndex(), buf.readableBytes() - 2, StandardCharsets.US_ASCII).trim());
                return position;
            case MSG_SET_IO:
                position.set(Position.KEY_RESULT,
                        String.valueOf(buf.readUnsignedByte()));
                return position;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedShort(); // data length

        String imei = String.format("%015d", buf.readLong());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedByte();

        if (type == MSG_RECORDS || type == MSG_EXTENDED_RECORDS) {

            List<Position> positions = new LinkedList<>();

            buf.readUnsignedByte(); // records left
            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position();
                position.setProtocol(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.setTime(new Date(buf.readUnsignedInt() * 1000));
                buf.readUnsignedByte(); // timestamp extension

                if (type == MSG_EXTENDED_RECORDS) {
                    buf.readUnsignedByte(); // record extension
                }

                buf.readUnsignedByte(); // priority (reserved)

                position.setValid(true);
                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);
                position.setAltitude(buf.readUnsignedShort() / 10.0);
                position.setCourse(buf.readUnsignedShort() / 100.0);

                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

                position.set(Position.KEY_HDOP, buf.readUnsignedByte() / 10.0);

                if (type == MSG_EXTENDED_RECORDS) {
                    position.set(Position.KEY_EVENT, buf.readUnsignedShort());
                } else {
                    position.set(Position.KEY_EVENT, buf.readUnsignedByte());
                }

                // Read 1 byte data
                int cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    position.set(Position.PREFIX_IO + id, buf.readUnsignedByte());
                }

                // Read 2 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    position.set(Position.PREFIX_IO + id, buf.readUnsignedShort());
                }

                // Read 4 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    position.set(Position.PREFIX_IO + id, buf.readUnsignedInt());
                }

                // Read 8 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    position.set(Position.PREFIX_IO + id, buf.readLong());
                }

                positions.add(position);
            }

            if (channel != null) {
                channel.write(ChannelBuffers.wrappedBuffer(DatatypeConverter.parseHexBinary("0002640113bc")));
            }

            return positions;

        } else if (type == MSG_DTCS) {

            List<Position> positions = new LinkedList<>();

            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position();
                position.setProtocol(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                buf.readUnsignedByte(); // reserved

                position.setTime(new Date(buf.readUnsignedInt() * 1000));

                position.setValid(true);
                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);

                if (buf.readUnsignedByte() == 2) {
                    position.set(Position.KEY_ARCHIVE, true);
                }

                position.set(Position.KEY_DTCS, buf.readBytes(5).toString(StandardCharsets.US_ASCII));

                positions.add(position);
            }

            if (channel != null) {
                channel.write(ChannelBuffers.wrappedBuffer(DatatypeConverter.parseHexBinary("00026d01c4a4")));
            }

            return positions;

        } else {

            return decodeCommandResponse(deviceSession, type, buf);

        }
    }

}
