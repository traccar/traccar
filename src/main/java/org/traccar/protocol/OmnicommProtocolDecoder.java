/*
 * Copyright 2019 - 2020 Anton Tananaev (anton@traccar.org)
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

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.protobuf.omnicomm.OmnicommMessageOuterClass;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class OmnicommProtocolDecoder extends BaseProtocolDecoder {

    public OmnicommProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_IDENTIFICATION = 0x80;
    public static final int MSG_ARCHIVE_INQUIRY = 0x85;
    public static final int MSG_ARCHIVE_DATA = 0x86;
    public static final int MSG_REMOVE_ARCHIVE_INQUIRY = 0x87;

    private OmnicommMessageOuterClass.OmnicommMessage parseProto(
            ByteBuf buf, int length) throws InvalidProtocolBufferException {

        final byte[] array;
        final int offset;
        if (buf.hasArray()) {
            array = buf.array();
            offset = buf.arrayOffset() + buf.readerIndex();
        } else {
            array = ByteBufUtil.getBytes(buf, buf.readerIndex(), length, false);
            offset = 0;
        }
        buf.skipBytes(length);

        return OmnicommMessageOuterClass.OmnicommMessage
                .getDefaultInstance().getParserForType().parseFrom(array, offset, length);
    }

    private void sendResponse(Channel channel, int type, long index) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0xC0);
            response.writeByte(type);
            response.writeShortLE(4);
            response.writeIntLE((int) index);
            response.writeShortLE(Checksum.crc16(Checksum.CRC16_CCITT_FALSE,
                    response.nioBuffer(1, response.writerIndex() - 1)));
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // prefix
        int type = buf.readUnsignedByte();
        buf.readUnsignedShortLE(); // length

        if (type == MSG_IDENTIFICATION) {

            getDeviceSession(channel, remoteAddress, String.valueOf(buf.readUnsignedIntLE()));
            sendResponse(channel, MSG_ARCHIVE_INQUIRY, 0);

        } else if (type == MSG_ARCHIVE_DATA) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            long index = buf.readUnsignedIntLE();
            buf.readUnsignedIntLE(); // time
            buf.readUnsignedByte(); // priority

            List<Position> positions = new LinkedList<>();

            while (buf.readableBytes() > 2) {

                OmnicommMessageOuterClass.OmnicommMessage message = parseProto(buf, buf.readUnsignedShortLE());

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                if (message.hasGeneral()) {
                    OmnicommMessageOuterClass.OmnicommMessage.General data = message.getGeneral();
                    position.set(Position.KEY_POWER, data.getUboard() * 0.1);
                    position.set(Position.KEY_BATTERY_LEVEL, data.getBatLife());
                    position.set(Position.KEY_IGNITION, BitUtil.check(data.getFLG(), 0));
                    position.set(Position.KEY_RPM, data.getTImp());
                }

                if (message.hasNAV()) {
                    OmnicommMessageOuterClass.OmnicommMessage.NAV data = message.getNAV();
                    position.setValid(true);
                    position.setTime(new Date((data.getGPSTime() + 1230768000) * 1000L)); // from 2009-01-01 12:00
                    position.setLatitude(data.getLAT() * 0.0000001);
                    position.setLongitude(data.getLON() * 0.0000001);
                    position.setSpeed(UnitsConverter.knotsFromKph(data.getGPSVel() * 0.1));
                    position.setCourse(data.getGPSDir());
                    position.setAltitude(data.getGPSAlt() * 0.1);
                    position.set(Position.KEY_SATELLITES, data.getGPSNSat());
                }

                if (message.hasLLSDt()) {
                    OmnicommMessageOuterClass.OmnicommMessage.LLSDt data = message.getLLSDt();
                    position.set("fuel1Temp", data.getTLLS1());
                    position.set("fuel1", data.getCLLS1());
                    position.set("fuel1State", data.getFLLS1());
                }

                if (position.getFixTime() != null) {
                    positions.add(position);
                }
            }

            if (positions.isEmpty()) {
                sendResponse(channel, MSG_REMOVE_ARCHIVE_INQUIRY, index + 1);
                return null;
            } else {
                return positions;
            }
        }

        return null;
    }

}
