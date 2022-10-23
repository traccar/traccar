/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Tk102ProtocolDecoder extends BaseProtocolDecoder {

    public Tk102ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN_REQUEST = 0x80;
    public static final int MSG_LOGIN_REQUEST_2 = 0x21;
    public static final int MSG_LOGIN_RESPONSE = 0x00;
    public static final int MSG_HEARTBEAT_REQUEST = 0xF0;
    public static final int MSG_HEARTBEAT_RESPONSE = 0xFF;
    public static final int MSG_REPORT_ONCE = 0x90;
    public static final int MSG_REPORT_INTERVAL = 0x93;

    public static final int MODE_GPRS = 0x30;
    public static final int MODE_GPRS_SMS = 0x33;

    private static final Pattern PATTERN = new PatternBuilder()
            .text("(")
            .expression("[A-Z]+")
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .expression("([AV])")                // validity
            .number("(dd)(dd.dddd)([NS])")       // latitude
            .number("(ddd)(dd.dddd)([EW])")      // longitude
            .number("(ddd.ddd)")                 // speed
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .text(")")
            .compile();

    private void sendResponse(Channel channel, int type, ByteBuf dataSequence, ByteBuf content) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte('[');
            response.writeByte(type);
            response.writeBytes(dataSequence);
            response.writeByte(content.readableBytes());
            response.writeBytes(content);
            content.release();
            response.writeByte(']');
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(1); // header
        int type = buf.readUnsignedByte();
        ByteBuf dataSequence = buf.readSlice(10);
        int length = buf.readUnsignedByte();

        if (type == MSG_LOGIN_REQUEST || type == MSG_LOGIN_REQUEST_2) {

            ByteBuf data = buf.readSlice(length);

            String id;
            if (type == MSG_LOGIN_REQUEST) {
                id =  data.toString(StandardCharsets.US_ASCII);
            } else {
                id = data.copy(1, 15).toString(StandardCharsets.US_ASCII);
            }

            if (getDeviceSession(channel, remoteAddress, id) != null) {
                ByteBuf response = Unpooled.buffer();
                response.writeByte(MODE_GPRS);
                response.writeBytes(data);
                sendResponse(channel, MSG_LOGIN_RESPONSE, dataSequence, response);
            }

        } else if (type == MSG_HEARTBEAT_REQUEST) {

            sendResponse(channel, MSG_HEARTBEAT_RESPONSE, dataSequence, buf.readRetainedSlice(length));

        } else {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Parser parser = new Parser(PATTERN, buf.readSlice(length).toString(StandardCharsets.US_ASCII));
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setSpeed(parser.nextDouble(0));

            dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
            position.setTime(dateBuilder.getDate());

            return position;

        }

        return null;
    }

}
