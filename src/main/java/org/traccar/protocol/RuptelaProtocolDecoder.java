/*
 * Copyright 2013 - 2021 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.DataConverter;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RuptelaProtocolDecoder extends BaseProtocolDecoder {

    private ByteBuf photo;

    public RuptelaProtocolDecoder(Protocol protocol) {
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
    public static final int MSG_FILES = 37;
    public static final int MSG_EXTENDED_RECORDS = 68;

    private Position decodeCommandResponse(DeviceSession deviceSession, int type, ByteBuf buf) {
        Position position = new Position(getProtocolName());
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

    private long readValue(ByteBuf buf, int length, boolean signed) {
        switch (length) {
            case 1:
                return signed ? buf.readByte() : buf.readUnsignedByte();
            case 2:
                return signed ? buf.readShort() : buf.readUnsignedShort();
            case 4:
                return signed ? buf.readInt() : buf.readUnsignedInt();
            default:
                return buf.readLong();
        }
    }

    private void decodeParameter(Position position, int id, ByteBuf buf, int length) {
        switch (id) {
            case 2:
            case 3:
            case 4:
                position.set("di" + (id - 1), readValue(buf, length, false));
                break;
            case 5:
                position.set(Position.KEY_IGNITION, readValue(buf, length, false) == 1);
                break;
            case 74:
                position.set(Position.PREFIX_TEMP + 3, readValue(buf, length, true) * 0.1);
                break;
            case 78:
            case 79:
            case 80:
                position.set(Position.PREFIX_TEMP + (id - 78), readValue(buf, length, true) * 0.1);
                break;
            case 198:
                position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
                break;
            case 199:
            case 200:
                position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                break;
            case 201:
                position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                break;
            default:
                position.set(Position.PREFIX_IO + id, readValue(buf, length, false));
                break;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

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
                Position position = new Position(getProtocolName());
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
                    decodeParameter(position, id, buf, 1);
                }

                // Read 2 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    decodeParameter(position, id, buf, 2);
                }

                // Read 4 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    decodeParameter(position, id, buf, 4);
                }

                // Read 8 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    decodeParameter(position, id, buf, 8);
                }

                Long driverIdPart1 = (Long) position.getAttributes().remove(Position.PREFIX_IO + 126);
                Long driverIdPart2 = (Long) position.getAttributes().remove(Position.PREFIX_IO + 127);
                if (driverIdPart1 != null && driverIdPart2 != null) {
                    ByteBuf driverId = Unpooled.copyLong(driverIdPart1, driverIdPart2);
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, driverId.toString(StandardCharsets.US_ASCII));
                    driverId.release();
                }

                positions.add(position);
            }

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        Unpooled.wrappedBuffer(DataConverter.parseHex("0002640113bc")), remoteAddress));
            }

            return positions;

        } else if (type == MSG_DTCS) {

            List<Position> positions = new LinkedList<>();

            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                buf.readUnsignedByte(); // reserved

                position.setTime(new Date(buf.readUnsignedInt() * 1000));

                position.setValid(true);
                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);

                if (buf.readUnsignedByte() == 2) {
                    position.set(Position.KEY_ARCHIVE, true);
                }

                position.set(Position.KEY_DTCS, buf.readSlice(5).toString(StandardCharsets.US_ASCII));

                positions.add(position);
            }

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        Unpooled.wrappedBuffer(DataConverter.parseHex("00026d01c4a4")), remoteAddress));
            }

            return positions;

        } else if (type == MSG_FILES) {

            int subtype = buf.readUnsignedByte();
            int source = buf.readUnsignedByte();

            if (subtype == 2) {
                ByteBuf filename = buf.readSlice(8);
                int total = buf.readUnsignedShort();
                int current = buf.readUnsignedShort();
                if (photo == null) {
                    photo = Unpooled.buffer();
                }
                photo.writeBytes(buf.readSlice(buf.readableBytes() - 2));
                if (current < total - 1) {
                    ByteBuf content = Unpooled.buffer();
                    content.writeByte(subtype);
                    content.writeByte(source);
                    content.writeBytes(filename);
                    content.writeShort(current + 1);
                    ByteBuf response = RuptelaProtocolEncoder.encodeContent(type, content);
                    content.release();
                    if (channel != null) {
                        channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                    }
                } else {
                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());
                    getLastLocation(position, null);
                    position.set(Position.KEY_IMAGE, Context.getMediaManager().writeFile(imei, photo, "jpg"));
                    photo.release();
                    photo = null;
                    return position;
                }
            }

            return null;

        } else {

            return decodeCommandResponse(deviceSession, type, buf);

        }
    }

}
