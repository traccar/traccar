/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class TlvProtocolDecoder extends BaseProtocolDecoder {

    public TlvProtocolDecoder(TlvProtocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, String type, String... arguments) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.dynamicBuffer();
            response.writeBytes(ChannelBuffers.copiedBuffer(type, StandardCharsets.US_ASCII));
            for (String argument : arguments) {
                response.writeByte(argument.length());
                response.writeBytes(ChannelBuffers.copiedBuffer(argument, StandardCharsets.US_ASCII));
            }
            response.writeByte(0);
            channel.write(response);
        }
    }

    private String readArgument(ChannelBuffer buf) {
        return buf.readBytes(buf.readUnsignedByte()).toString(StandardCharsets.US_ASCII);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        String type = buf.readBytes(2).toString(StandardCharsets.US_ASCII);

        if (channel != null) {
            switch (type) {
                case "0A":
                case "0C":
                    sendResponse(channel, type);
                    break;
                case "0B":
                    sendResponse(channel, type, "1482202689", "10", "20", "15");
                    break;
                case "0E":
                case "0F":
                    sendResponse(channel, type, "30", "Unknown");
                    break;
                default:
                    break;
            }
        }

        if (type.equals("0E")) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, readArgument(buf));
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setValid(true);
            position.setTime(new Date(Long.parseLong(readArgument(buf)) * 1000));

            readArgument(buf); // location identifier

            position.setLongitude(Double.parseDouble(readArgument(buf)));
            position.setLatitude(Double.parseDouble(readArgument(buf)));
            position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(readArgument(buf))));
            position.setCourse(Double.parseDouble(readArgument(buf)));

            position.set(Position.KEY_SATELLITES, Integer.parseInt(readArgument(buf)));

            return position;

        }

        return null;
    }

}
