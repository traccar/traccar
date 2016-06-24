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
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class HuaShengProtocolDecoder extends BaseProtocolDecoder {

    public HuaShengProtocolDecoder(HuaShengProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_POSITION = 0xAA00;
    public static final int MSG_POSITION_RSP = 0xFF01;
    public static final int MSG_LOGIN = 0xAA02;
    public static final int MSG_LOGIN_RSP = 0xFF03;

    private static void sendResponse(Channel channel, int type, ChannelBuffer content) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.dynamicBuffer();
            response.writeByte(0xC0);
            response.writeShort(0x0100);
            response.writeShort(0x000D); // TODO: length
            response.writeByte(MSG_LOGIN_RSP);
            response.writeShort(0);
            response.writeInt(1);
            response.writeByte(0); // success
            response.writeByte(0xC0);
            channel.write(response);
        }
    }


    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(1); // start marker
        buf.readUnsignedByte(); // flag
        buf.readUnsignedByte(); // reserved
        buf.readUnsignedShort(); // length

        int type = buf.readUnsignedShort();

        buf.readUnsignedShort(); // checksum
        buf.readUnsignedInt(); // index

        if (type == MSG_LOGIN) {

            while (buf.readableBytes() > 4) {
                int subtype = buf.readUnsignedShort();
                int length = buf.readUnsignedShort() - 4;
                if (subtype == 0x0003) {
                    String imei = buf.readBytes(length).toString(StandardCharsets.US_ASCII);
                    if (identify(imei, channel, remoteAddress) && channel != null) {

                    }
                } else {
                    buf.skipBytes(length);
                }
            }

        } else if (type == MSG_POSITION) {



        }



        /*buf.skipBytes(2); // header
        buf.readByte(); // size

        Position position = new Position();
        position.setProtocol(getProtocolName());

        // Zero for location messages
        int power = buf.readUnsignedByte();
        int gsm = buf.readUnsignedByte();

        String imei = ChannelBuffers.hexDump(buf.readBytes(8)).substring(1);
        if (!identify(imei, channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.set(Position.KEY_INDEX, buf.readUnsignedShort());

        int type = buf.readUnsignedByte();

        if (type == MSG_HEARTBEAT) {

            getLastLocation(position, null);

            position.set(Position.KEY_POWER, power);
            position.set(Position.KEY_GSM, gsm);

            if (channel != null) {
                byte[] response = {0x54, 0x68, 0x1A, 0x0D, 0x0A};
                channel.write(ChannelBuffers.wrappedBuffer(response));
            }

        } else if (type == MSG_DATA) {

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
            position.setTime(dateBuilder.getDate());

            double latitude = buf.readUnsignedInt() / (60.0 * 30000.0);
            double longitude = buf.readUnsignedInt() / (60.0 * 30000.0);

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            position.setCourse(buf.readUnsignedShort());

            buf.skipBytes(3); // reserved

            long flags = buf.readUnsignedInt();
            position.setValid(BitUtil.check(flags, 0));
            if (!BitUtil.check(flags, 1)) {
                latitude = -latitude;
            }
            if (!BitUtil.check(flags, 2)) {
                longitude = -longitude;
            }

            position.setLatitude(latitude);
            position.setLongitude(longitude);

        } else {

            return null;

        }*/

        return null;
    }

}
