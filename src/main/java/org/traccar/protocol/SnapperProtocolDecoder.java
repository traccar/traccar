/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class SnapperProtocolDecoder extends BaseProtocolDecoder {

    public SnapperProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_HELLO = 0x01;
    public static final int MSG_SENDING_START = 0x02;
    public static final int MSG_SENDING_FINISH = 0x03;
    public static final int MSG_SEND_EVENTS = 0x21;
    public static final int MSG_SEND_TECH_INFO = 0x23;
    public static final int MSG_UPDATE_CMS_NUM = 0x24;
    public static final int MSG_SEND_SYSTEM_INFO = 0x26;
    public static final int MSG_SEND_USER_PHONE_NUMBERS = 0x31;
    public static final int MSG_SEND_GPS_DATA = 0x32;
    public static final int MSG_SEND_LBS_DATA = 0x33;
    public static final int MSG_SEND_SYSTEM_STATUS = 0x34;
    public static final int MSG_SEND_TRANSIT_SETTINGS = 0x35;
    public static final int MSG_GET_SETTINGS = 0x36;
    public static final int MSG_SEND_CONCATENATED_PACKET = 0x37;
    public static final int MSG_SEND_DEBUG_INFO = 0x38;

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, int index, int type, String answer) {

        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte('K');
            response.writeByte(3); // protocol version
            response.writeLongLE(0); // reserved
            response.writeShortLE(0); // encryption
            response.writeIntLE(answer.length());
            response.writeIntLE(0); // reserved
            response.writeShortLE(index);
            response.writeByte(Checksum.sum(ByteBuffer.wrap(answer.getBytes(StandardCharsets.US_ASCII))));
            response.writeShortLE(type);
            response.writeCharSequence(answer, StandardCharsets.US_ASCII);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private void decodeEvents(Position position, ByteBuf buf) {

        position.set(Position.KEY_EVENT, buf.readUnsignedByte());
        buf.readUnsignedByte(); // info 1
        buf.readUnsignedByte(); // info 2
        buf.readUnsignedIntLE(); // timestamp
        buf.readUnsignedByte(); // timezone
    }

    private void decodeTechInfo(Position position, ByteBuf buf) {

        for (int i = 0; i < 5; i++) {
            buf.readUnsignedByte(); // index
            int type = buf.readUnsignedByte();
            switch (type) {
                case 0x00 -> {
                    position.set(Position.KEY_POWER, buf.readUnsignedByte() * 0.1);
                    position.set(Position.KEY_DEVICE_TEMP, buf.readByte());
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                }
                case 0x01 -> {
                    position.set("interiorTemp", buf.readByte());
                    position.set("engineTemp", buf.readByte());
                    buf.readUnsignedByte(); // reserved
                }
                default -> buf.skipBytes(3);
            }
        }
    }

    private void decodeGpsData(Position position, ByteBuf buf) throws ParseException {

        String content = buf.readCharSequence(buf.readableBytes(), StandardCharsets.US_ASCII).toString();
        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        int flags = Integer.parseInt(json.getString("f"), 16);
        if (!BitUtil.check(flags, 3)) {
            return;
        }

        position.setValid(BitUtil.check(flags, 1));
        if (!BitUtil.check(flags, 6)) {
            position.setLatitude(-position.getLatitude());
        }
        if (!BitUtil.check(flags, 7)) {
            position.setLongitude(-position.getLongitude());
        }

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(json.getString("d") + json.getString("t").split("\\.")[0]));

        String lat = json.getString("la");
        position.setLatitude(Integer.parseInt(lat.substring(0, 2)) + Double.parseDouble(lat.substring(2)) / 60);
        String lon = json.getString("lo");
        position.setLongitude(Integer.parseInt(lon.substring(0, 3)) + Double.parseDouble(lon.substring(3)) / 60);

        position.setAltitude(Double.parseDouble(json.getString("a")));
        position.setSpeed(Double.parseDouble(json.getString("s")));
        position.setCourse(Double.parseDouble(json.getString("c")));

        position.set(Position.KEY_SATELLITES, Integer.parseInt(json.getString("sv")));
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        byte header = buf.readByte();
        if (header == 'P') {
            if (channel != null) {
                ByteBuf response = Unpooled.wrappedBuffer(new byte[] {0x50, 0x4f});
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }
            return null;
        }

        buf.readUnsignedByte(); // protocol version
        buf.readUnsignedIntLE(); // system bonus identifier

        String serialNumber = String.valueOf(buf.readUnsignedIntLE());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, serialNumber);
        if (deviceSession == null) {
            return null;
        }

        buf.readUnsignedShortLE(); // encryption
        int length = buf.readIntLE();
        buf.readUnsignedByte(); // flags
        buf.readUnsignedMediumLE(); // reserved
        int index = buf.readUnsignedShortLE();
        buf.readUnsignedByte(); // checksum
        int type = buf.readUnsignedShortLE();

        if (type == MSG_HELLO) {
            sendResponse(channel, remoteAddress, index, type, "hello");
        } else {
            sendResponse(channel, remoteAddress, index, type, "OK");
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        return switch (type) {
            case MSG_SEND_EVENTS -> {
                decodeEvents(position, buf);
                getLastLocation(position, null); // TODO read timestamp
                yield position;
            }
            case MSG_SEND_TECH_INFO -> {
                decodeTechInfo(position, buf);
                getLastLocation(position, null);
                yield position;
            }
            case MSG_SEND_GPS_DATA -> {
                decodeGpsData(position, buf.readSlice(length));
                yield position;
            }
            case MSG_SEND_CONCATENATED_PACKET -> {
                int count = buf.readUnsignedShortLE();
                for (int i = 0; i < count; i++) {
                    int partType = buf.readUnsignedShortLE();
                    int partLength = buf.readUnsignedShortLE();
                    switch (partType) {
                        case MSG_SEND_EVENTS:
                            decodeEvents(position, buf);
                            break;
                        case MSG_SEND_TECH_INFO:
                            decodeTechInfo(position, buf);
                            break;
                        case MSG_SEND_GPS_DATA:
                            decodeGpsData(position, buf.readSlice(partLength));
                            break;
                        default:
                            buf.skipBytes(partLength);
                            break;
                    }
                }
                if (position.getFixTime() == null) {
                    getLastLocation(position, null);
                }
                yield position;
            }
            default -> null;
        };
    }

}
