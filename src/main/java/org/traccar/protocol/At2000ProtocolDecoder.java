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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.DataConverter;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class At2000ProtocolDecoder extends BaseProtocolDecoder {

    private static final int BLOCK_LENGTH = 16;

    public At2000ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_ACKNOWLEDGEMENT = 0x00;
    public static final int MSG_DEVICE_ID = 0x01;
    public static final int MSG_TRACK_REQUEST = 0x88;
    public static final int MSG_TRACK_RESPONSE = 0x89;
    public static final int MSG_SESSION_END = 0x0c;

    private Cipher cipher;

    private static void sendRequest(Channel channel) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer(BLOCK_LENGTH);
            response.writeByte(MSG_TRACK_REQUEST);
            response.writeMedium(0);
            response.writerIndex(BLOCK_LENGTH);
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getUnsignedByte(buf.readerIndex()) == 0x01) {
            buf.readUnsignedByte(); // codec id
        }

        int type = buf.readUnsignedByte();
        buf.readUnsignedMediumLE(); // length
        buf.skipBytes(BLOCK_LENGTH - 1 - 3);

        if (type == MSG_DEVICE_ID) {

            String imei = buf.readSlice(15).toString(StandardCharsets.US_ASCII);
            if (getDeviceSession(channel, remoteAddress, imei) != null) {

                byte[] iv = new byte[BLOCK_LENGTH];
                buf.readBytes(iv);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);

                SecretKeySpec keySpec = new SecretKeySpec(
                        DataConverter.parseHex("000102030405060708090a0b0c0d0e0f"), "AES");

                cipher = Cipher.getInstance("AES/CBC/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

                byte[] data = new byte[BLOCK_LENGTH];
                buf.readBytes(data);
                cipher.update(data);

            }

        } else if (type == MSG_TRACK_RESPONSE) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            if (buf.capacity() <= BLOCK_LENGTH) {
                return null; // empty message
            }

            List<Position> positions = new LinkedList<>();

            byte[] data = new byte[buf.capacity() - BLOCK_LENGTH];
            buf.readBytes(data);
            buf = Unpooled.wrappedBuffer(cipher.update(data));
            try {
                while (buf.readableBytes() >= 63) {

                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());

                    buf.readUnsignedShortLE(); // index
                    buf.readUnsignedShortLE(); // reserved

                    position.setValid(true);

                    position.setTime(new Date(buf.readLongLE() * 1000));

                    position.setLatitude(buf.readFloatLE());
                    position.setLongitude(buf.readFloatLE());
                    position.setAltitude(buf.readFloatLE());
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readFloatLE()));
                    position.setCourse(buf.readFloatLE());

                    buf.readUnsignedIntLE(); // geozone event
                    buf.readUnsignedIntLE(); // io events
                    buf.readUnsignedIntLE(); // geozone value
                    buf.readUnsignedIntLE(); // io values
                    buf.readUnsignedShortLE(); // operator

                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShortLE());
                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShortLE());

                    position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.001);

                    buf.readUnsignedShortLE(); // cid
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    buf.readUnsignedByte(); // current profile

                    position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
                    position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedByte());
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

                    positions.add(position);

                }
            } finally {
                buf.release();
            }

            return positions;

        }

        if (type == MSG_DEVICE_ID) {
            sendRequest(channel);
        }

        return null;
    }

}
