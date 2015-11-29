/*
 * Copyright 2015 Iuri Pereira (iuricmp@gmail.com)
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

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class GeoStudioProtocolDecoder extends BaseProtocolDecoder {

    public GeoStudioProtocolDecoder(GeoStudioProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        HttpRequest request = (HttpRequest) msg;
        QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());

        Map<String, List<String>> params = decoder.getParameters();
        if (params.isEmpty()) {
            decoder = new QueryStringDecoder(request.getContent().toString(Charset.defaultCharset()), false);
            params = decoder.getParameters();
        }

        if (HttpMethod.POST.equals(request.getMethod())) {
            return decodePostRequest(request.getContent(), channel, remoteAddress);
        }
        
        return null;
    }

    @SuppressWarnings("unused")
    private Object decodePostRequest(ChannelBuffer content, Channel channel, SocketAddress remoteAddress) {

        int byteIndex = 3; // Indexador do Buffer
        int byteLen = content.capacity();// Tamanho do buffer

        // Start Header
        content.resetReaderIndex();
        if (content.readShort() != 0x0310) {
            return null; // invalid pkg
        }

        byte firmwareVersion = content.readByte();
        byte packageSize = content.readByte();
        int packageId = content.readShort();

        switch (packageId) {
            case 0x00A4: {
                int tag = content.readUnsignedByte();
                switch (tag) {
                    case 0xF1:
                        Position position = new Position();
                        position.setProtocol(getProtocolName());

                        int tagStartIndex = content.readerIndex();
                        short tagSize = content.readUnsignedByte();
                        int tagEndIndex = tagStartIndex + tagSize;
                        Integer id = content.readInt();

                        // Identification
                        if (!identify(id.toString(), channel, remoteAddress)) {
                            return null;
                        }
                        sendResponse(channel, remoteAddress);
                        position.setDeviceId(getDeviceId());

                        // Decode position
                        position.setValid(true);
                        double latitude = ((double) content.readInt() / 6000000); // -16.703058
                        double longitude = ((double) content.readInt() / 6000000); // -49.327972
                        position.setLatitude(latitude);
                        position.setLongitude(longitude);

                        int ts = content.readInt();
                        position.setTime(new Date(ts * 1000L));// *1000 is to convert seconds to milliseconds

                        position.setSpeed(content.readShort() / 10);
                        position.setAltitude(content.readShort());
                        position.set(Event.KEY_IGNITION, content.readByte() == 1);

                        byte releStatus = content.readByte();

                        Byte sensorStatus = content.readByte();
                        position.set(Event.PREFIX_IO + 1, getBit(sensorStatus, 1));
                        position.set(Event.PREFIX_IO + 2, getBit(sensorStatus, 2));
                        position.set(Event.PREFIX_IO + 3, getBit(sensorStatus, 3));
                        position.set(Event.PREFIX_IO + 4, getBit(sensorStatus, 4));

                        content.skipBytes(1);

                        ChannelBuffer labelCode = content.readBytes(tagEndIndex - content.readerIndex() + 1);
                        String labelCodeDecoded = new String(labelCode.array());

                        System.out.println(position.getDeviceTime() + "    labelCode:" + labelCodeDecoded);
                        // System.out.println("1: " + position.getOther().get(Event.PREFIX_IO + 1));
                        // System.out.println("2: " + position.getOther().get(Event.PREFIX_IO + 2));
                        // System.out.println("3: " + position.getOther().get(Event.PREFIX_IO + 3));
                        // System.out.println("4: " + position.getOther().get(Event.PREFIX_IO + 4));

                        return position;
                    // break;
                    
                    default:
                        break;
                }
            }
                break;

            case 0x00A5:
                // ByteIndex += 2;
                // PacketLen = ByteToShort16(ByteArray, ByteIndex);//PackekLen especial de 16 bits
                // ByteIndex += 2;
                // FMI_Get_DataLogger(ByteArray, ByteIndex, PacketLen, listaRastreamento);
                // PacketLen += 2;
                extractDataLoggerData(content);
                break;

            default:
                return null;
        }
        
        return null;
    }

    private void extractDataLoggerData(ChannelBuffer content) {
        // TODO Auto-generated method stub

    }

    private int getBit(byte value, int position) {
        return (value >> position) & 1;
    }

    private static void sendResponse(Channel channel, SocketAddress remoteAddress) {
        if (channel != null) {
            byte[] ackMessage = "<tracking></tracking>".getBytes();
            channel.write(ChannelBuffers.wrappedBuffer(ackMessage), remoteAddress);
        }
    }

}
