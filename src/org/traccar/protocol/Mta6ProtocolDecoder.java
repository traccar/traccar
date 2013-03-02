/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class Mta6ProtocolDecoder extends BaseProtocolDecoder {
    
    private boolean simple;

    public Mta6ProtocolDecoder(ServerManager serverManager, boolean simple) {
        super(serverManager);
        this.simple = simple;
    }

    private void sendContinue(Channel channel) {
        HttpResponse response = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        channel.write(response);
    }
    
    private void sendResponse(Channel channel, short packetId, short packetCount) {
        HttpResponse response = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        ChannelBuffer begin = ChannelBuffers.copiedBuffer("#ACK#", Charset.defaultCharset());
        ChannelBuffer end = ChannelBuffers.directBuffer(3);
        end.writeByte(packetId);
        end.writeByte(packetCount);
        end.writeByte(0);
        
        response.setContent(ChannelBuffers.wrappedBuffer(begin, end));
        channel.write(response);
    }
    
    private static boolean checkBit(long mask, int bit) {
        long checkMask = 1 << bit;
        return (mask & checkMask) == checkMask;
    }
    
    private static class FloatReader {
        
        private int previousFloat;
        
        public float readFloat(ChannelBuffer buf) {
            switch (buf.getUnsignedByte(buf.readerIndex()) >> 6)
            {
                case 0:
                    previousFloat = buf.readInt() << 2;
                    break;
                case 1:
                    previousFloat = (previousFloat & 0xffffff00) + ((buf.readUnsignedByte() & 0x3f) << 2);
                    break;
                case 2:
                    previousFloat = (previousFloat & 0xffff0000) + ((buf.readUnsignedShort() & 0x3fff) << 2);
                    break;
                case 3:
                    previousFloat = (previousFloat & 0xff000000) + ((buf.readUnsignedMedium() & 0x3fffff) << 2);
                    break;
            }
            return Float.intBitsToFloat(previousFloat);
        }
        
    }
    
    private static class TimeReader extends FloatReader {
        
        private long weekNumber;
        
        public Date readTime(ChannelBuffer buf) {
            long weekTime = (long) (readFloat(buf) * 1000);
            if (weekNumber == 0) {
                weekNumber = buf.readUnsignedShort();
            }

            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, 1980);
            time.set(Calendar.MONTH, 0);
            time.set(Calendar.DAY_OF_MONTH, 6);
            long offset = time.getTimeInMillis();

            return new Date(offset + weekNumber * 7 * 24 * 60 * 60 * 1000 + weekTime);
        }
        
    }

    private List<Position> parseFormatA(ChannelBuffer buf, long deviceId) {
        List<Position> positions = new LinkedList<Position>();
        
        FloatReader latitudeReader = new FloatReader();
        FloatReader longitudeReader = new FloatReader();
        TimeReader timeReader = new TimeReader();
        
        try {
            while (buf.readable()) {
                Position position = new Position();
                position.setDeviceId(deviceId);
                StringBuilder extendedInfo = new StringBuilder("<protocol>mta6</protocol>");

                short flags = buf.readUnsignedByte();

                // Skip events
                short event = buf.readUnsignedByte();
                if (checkBit(event, 7)) {
                    if (checkBit(event, 6)) {
                        buf.skipBytes(8);
                    } else {
                        while (checkBit(event, 7)) {
                            event = buf.readUnsignedByte();
                        }
                    }
                }

                position.setLatitude(latitudeReader.readFloat(buf) / Math.PI * 180);
                position.setLongitude(longitudeReader.readFloat(buf) / Math.PI * 180);
                position.setTime(timeReader.readTime(buf));

                if (checkBit(flags, 0)) {
                    buf.readUnsignedByte(); // status
                }

                if (checkBit(flags, 1)) {
                    position.setAltitude((double) buf.readUnsignedShort());
                }

                if (checkBit(flags, 2)) {
                    position.setSpeed((double) (buf.readUnsignedShort() & 0x03ff));
                    position.setCourse((double) buf.readUnsignedByte());
                }

                if (checkBit(flags, 3)) {
                    extendedInfo.append("<milage>");
                    extendedInfo.append(buf.readUnsignedShort());
                    extendedInfo.append("</milage>");
                }

                if (checkBit(flags, 4)) {
                    extendedInfo.append("<fuel1>").append(buf.readUnsignedInt()).append("</fuel1>");
                    extendedInfo.append("<fuel2>").append(buf.readUnsignedInt()).append("</fuel2>");
                    extendedInfo.append("<hours1>").append(buf.readUnsignedShort()).append("</hours1>");
                    extendedInfo.append("<hours2>").append(buf.readUnsignedShort()).append("</hours2>");
                }

                if (checkBit(flags, 5)) {
                    extendedInfo.append("<adc1>").append(buf.readUnsignedShort() & 0x03ff).append("</adc1>");
                    extendedInfo.append("<adc2>").append(buf.readUnsignedShort() & 0x03ff).append("</adc2>");
                    extendedInfo.append("<adc3>").append(buf.readUnsignedShort() & 0x03ff).append("</adc3>");
                    extendedInfo.append("<adc4>").append(buf.readUnsignedShort() & 0x03ff).append("</adc4>");
                }

                if (checkBit(flags, 6)) {
                    extendedInfo.append("<temperature>");
                    extendedInfo.append(buf.readByte());
                    extendedInfo.append("</temperature>");
                    buf.getUnsignedByte(buf.readerIndex()); // control (>> 4)
                    extendedInfo.append("<sensor>");
                    extendedInfo.append(buf.readUnsignedShort() & 0x0fff);
                    extendedInfo.append("</sensor>");
                    buf.readUnsignedShort(); // old sensor state (& 0x0fff)
                }

                if (checkBit(flags, 7)) {
                    extendedInfo.append("<battery>");
                    extendedInfo.append(buf.getUnsignedByte(buf.readerIndex()) >> 2);
                    extendedInfo.append("</battery>");
                    position.setPower((double) (buf.readUnsignedShort() & 0x03ff));
                    buf.readByte(); // microcontroller temperature

                    extendedInfo.append("<gsm>");
                    extendedInfo.append((buf.getUnsignedByte(buf.readerIndex()) >> 4) & 0x07);
                    extendedInfo.append("</gsm>");

                    int satellites = buf.readUnsignedByte() & 0x0f;
                    position.setValid(satellites >= 3);
                    extendedInfo.append("<satellites>").append(satellites).append("</satellites>");
                }

                position.setExtendedInfo(extendedInfo.toString());
                positions.add(position);
            }
        } catch (IndexOutOfBoundsException error) {
        }
        
        return positions;
    }

    private Position parseFormatA1(ChannelBuffer buf, long deviceId) {
        Position position = new Position();
        position.setDeviceId(deviceId);
        StringBuilder extendedInfo = new StringBuilder("<protocol>mta6can</protocol>");

        short flags = buf.readUnsignedByte();

        // Skip events
        short event = buf.readUnsignedByte();
        if (checkBit(event, 7)) {
            if (checkBit(event, 6)) {
                buf.skipBytes(8);
            } else {
                while (checkBit(event, 7)) {
                    event = buf.readUnsignedByte();
                }
            }
        }

        position.setLatitude(new FloatReader().readFloat(buf) / Math.PI * 180);
        position.setLongitude(new FloatReader().readFloat(buf) / Math.PI * 180);
        position.setTime(new TimeReader().readTime(buf));

        buf.readUnsignedByte(); // status

        if (checkBit(flags, 0)) {
            position.setAltitude((double) buf.readUnsignedShort());
            position.setSpeed((double) buf.readUnsignedByte());
            position.setCourse((double) buf.readByte());
            extendedInfo.append("<milage>");
            extendedInfo.append(new FloatReader().readFloat(buf));
            extendedInfo.append("</milage>");
        }

        if (checkBit(flags, 1)) {
            new FloatReader().readFloat(buf); // fuel consumtion
            extendedInfo.append("<hours>").append(new FloatReader().readFloat(buf)).append("</hours>");
            extendedInfo.append("<tank>").append(buf.readUnsignedByte() * 0.4).append("</tank>");
        }

        if (checkBit(flags, 2)) {
            extendedInfo.append("<engine>").append(buf.readUnsignedShort() * 0.125).append("</engine>");
            extendedInfo.append("<pedals>").append(buf.readUnsignedByte()).append("</pedals>");
            extendedInfo.append("<temperature>").append(buf.readUnsignedByte() - 40).append("</temperature>");
            buf.readUnsignedShort(); // service milage
        }

        if (checkBit(flags, 3)) {
            extendedInfo.append("<fuel>").append(buf.readUnsignedShort()).append("</fuel>");
            extendedInfo.append("<adc2>").append(buf.readUnsignedShort()).append("</adc2>");
            extendedInfo.append("<adc3>").append(buf.readUnsignedShort()).append("</adc3>");
            extendedInfo.append("<adc4>").append(buf.readUnsignedShort()).append("</adc4>");
        }

        if (checkBit(flags, 4)) {
            extendedInfo.append("<temperature>");
            extendedInfo.append(buf.readByte());
            extendedInfo.append("</temperature>");
            buf.getUnsignedByte(buf.readerIndex()); // control (>> 4)
            extendedInfo.append("<sensor>");
            extendedInfo.append(buf.readUnsignedShort() & 0x0fff);
            extendedInfo.append("</sensor>");
            buf.readUnsignedShort(); // old sensor state (& 0x0fff)
        }

        if (checkBit(flags, 5)) {
            extendedInfo.append("<battery>");
            extendedInfo.append(buf.getUnsignedByte(buf.readerIndex()) >> 2);
            extendedInfo.append("</battery>");
            position.setPower((double) (buf.readUnsignedShort() & 0x03ff));
            buf.readByte(); // microcontroller temperature

            extendedInfo.append("<gsm>");
            extendedInfo.append(buf.getUnsignedByte(buf.readerIndex()) >> 5);
            extendedInfo.append("</gsm>");

            int satellites = buf.readUnsignedByte() & 0x1f;
            position.setValid(satellites >= 3);
            extendedInfo.append("<satellites>").append(satellites).append("</satellites>");
        }
        
        // TODO: process other data

        position.setExtendedInfo(extendedInfo.toString());
        
        return position;
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        HttpRequest request = (HttpRequest) msg;

        ChannelBuffer buf = request.getContent();
        int length = buf.readableBytes();

        // Read identifier
        buf.skipBytes("id=".length());
        int index = ChannelBufferTools.find(buf, buf.readerIndex(), length, "&");
        String uniqueId = buf.toString(buf.readerIndex(), index - buf.readerIndex(), Charset.defaultCharset());
        long deviceId;
        try {
            deviceId = getDataManager().getDeviceByImei(uniqueId).getId();
        } catch(Exception error) {
            Log.warning("Unknown device - " + uniqueId);
            return null;
        }
        buf.skipBytes(uniqueId.length());
        buf.skipBytes("&bin=".length());
        
        // Read header
        short packetId = buf.readUnsignedByte();
        short offset = buf.readUnsignedByte(); // dataOffset
        short packetCount = buf.readUnsignedByte();
        buf.readUnsignedByte(); // reserved
        short parameters = buf.readUnsignedByte(); // TODO: handle timezone
        buf.skipBytes(offset - 5);
        
        // Send response
        if (channel != null) {
            sendContinue(channel);
            sendResponse(channel, packetId, packetCount);
        }
        
        // Parse data
        if (packetId == 0x31 || packetId == 0x32 || packetId == 0x36) {
            if (simple) {
                return parseFormatA1(buf, deviceId);
            } else {
                return parseFormatA(buf, deviceId);
            }
        } //else if (0x34 0x38 0x4F 0x59)

        return null;
    }

}
