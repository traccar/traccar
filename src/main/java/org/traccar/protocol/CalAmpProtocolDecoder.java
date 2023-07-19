/*
 * Copyright 2015 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class CalAmpProtocolDecoder extends BaseProtocolDecoder {

    public CalAmpProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_NULL = 0;
    public static final int MSG_ACK = 1;
    public static final int MSG_EVENT_REPORT = 2;
    public static final int MSG_ID_REPORT = 3;
    public static final int MSG_USER_DATA = 4;
    public static final int MSG_APP_DATA = 5;
    public static final int MSG_CONFIG = 6;
    public static final int MSG_UNIT_REQUEST = 7;
    public static final int MSG_LOCATE_REPORT = 8;
    public static final int MSG_USER_DATA_ACC = 9;
    public static final int MSG_MINI_EVENT_REPORT = 10;
    public static final int MSG_MINI_USER_DATA = 11;

    public static final int SERVICE_UNACKNOWLEDGED = 0;
    public static final int SERVICE_ACKNOWLEDGED = 1;
    public static final int SERVICE_RESPONSE = 2;

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int type, int index, int result) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer(10);
            response.writeByte(SERVICE_RESPONSE);
            response.writeByte(MSG_ACK);
            response.writeShort(index);
            response.writeByte(type);
            response.writeByte(result);
            response.writeByte(0);
            response.writeMedium(0);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private Position decodePosition(DeviceSession deviceSession, int type, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(new Date(buf.readUnsignedInt() * 1000));
        if (type != MSG_MINI_EVENT_REPORT) {
            buf.readUnsignedInt(); // fix time
        }
        position.setLatitude(buf.readInt() * 0.0000001);
        position.setLongitude(buf.readInt() * 0.0000001);
        if (type != MSG_MINI_EVENT_REPORT) {
            position.setAltitude(buf.readInt() * 0.01);
            position.setSpeed(UnitsConverter.knotsFromCps(buf.readUnsignedInt()));
        }
        position.setCourse(buf.readShort());
        if (type == MSG_MINI_EVENT_REPORT) {
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        }

        if (type == MSG_MINI_EVENT_REPORT) {
            position.set(Position.KEY_SATELLITES, buf.getUnsignedByte(buf.readerIndex()) & 0xf);
            position.setValid((buf.readUnsignedByte() & 0x20) == 0);
        } else {
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.setValid((buf.readUnsignedByte() & 0x08) == 0);
        }

        if (type != MSG_MINI_EVENT_REPORT) {
            position.set("carrier", buf.readUnsignedShort());
            position.set(Position.KEY_RSSI, buf.readShort());
        }

        position.set("modem", buf.readUnsignedByte());

        if (type != MSG_MINI_EVENT_REPORT) {
            position.set(Position.KEY_HDOP, buf.readUnsignedByte());
        }

        int input = buf.readUnsignedByte();
        position.set(Position.KEY_INPUT, input);
        position.set(Position.KEY_IGNITION, BitUtil.check(input, 0));

        if (type != MSG_MINI_EVENT_REPORT) {
            position.set(Position.KEY_STATUS, buf.readUnsignedByte());
        }

        if (type == MSG_EVENT_REPORT || type == MSG_MINI_EVENT_REPORT) {
            if (type != MSG_MINI_EVENT_REPORT) {
                buf.readUnsignedByte(); // event index
            }
            position.set(Position.KEY_EVENT, buf.readUnsignedByte());
        }

        if (type == MSG_EVENT_REPORT || type == MSG_LOCATE_REPORT || type == MSG_MINI_EVENT_REPORT) {

            int accType = BitUtil.from(buf.getUnsignedByte(buf.readerIndex()), 6);
            int accCount = BitUtil.to(buf.readUnsignedByte(), 6);

            if (type != MSG_MINI_EVENT_REPORT) {
                position.set("append", buf.readUnsignedByte());
            }

            if (accType == 1) {
                buf.readUnsignedInt(); // threshold
                buf.readUnsignedInt(); // mask
            }

            for (int i = 0; i < accCount; i++) {
                if (buf.readableBytes() >= 4) {
                    position.set("acc" + i, buf.readUnsignedInt());
                }
            }

        } else if (type == MSG_USER_DATA) {

            buf.readUnsignedByte(); // message route
            buf.readUnsignedByte(); // message id
            position.set(
                    Position.KEY_RESULT,
                    buf.readCharSequence(buf.readUnsignedShort(), StandardCharsets.US_ASCII).toString().trim());

        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (BitUtil.check(buf.getByte(buf.readerIndex()), 7)) {

            int content = buf.readUnsignedByte();

            if (BitUtil.check(content, 0)) {
                String id = ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedByte())).replace("f", "");
                getDeviceSession(channel, remoteAddress, id);
            }

            if (BitUtil.check(content, 1)) {
                buf.skipBytes(buf.readUnsignedByte()); // identifier type
            }

            if (BitUtil.check(content, 2)) {
                buf.skipBytes(buf.readUnsignedByte()); // authentication
            }

            if (BitUtil.check(content, 3)) {
                buf.skipBytes(buf.readUnsignedByte()); // routing
            }

            if (BitUtil.check(content, 4)) {
                buf.skipBytes(buf.readUnsignedByte()); // forwarding
            }

            if (BitUtil.check(content, 5)) {
                buf.skipBytes(buf.readUnsignedByte()); // response redirection
            }

        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        int service = buf.readUnsignedByte();
        int type = buf.readUnsignedByte();
        int index = buf.readUnsignedShort();

        if (service == SERVICE_ACKNOWLEDGED) {
            sendResponse(channel, remoteAddress, type, index, 0);
        }

        if (type == MSG_EVENT_REPORT || type == MSG_LOCATE_REPORT
                || type == MSG_MINI_EVENT_REPORT || type == MSG_USER_DATA) {
            return decodePosition(deviceSession, type, buf);
        }

        return null;
    }

}
