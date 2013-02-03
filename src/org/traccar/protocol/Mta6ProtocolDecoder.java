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

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
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

    public Mta6ProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
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
    
    private List<Position> parseFormatA(ChannelBuffer buf, long deviceId) {
        List<Position> positions = new LinkedList<Position>();
        
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
            
            position.setLatitude((double) Float.intBitsToFloat(buf.readInt() << 2));
            position.setLongitude((double) Float.intBitsToFloat(buf.readInt() << 2));
            
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.setTimeInMillis((buf.readUnsignedInt() << 2) * 1000);
            position.setTime(time.getTime());
            
            buf.readUnsignedShort(); // week
            
            
            position.setExtendedInfo(extendedInfo.toString());
            positions.add(position);
        }
        
        return positions;
    }
    
    private List<Position> parseFormatB(ChannelBuffer buf, long deviceId) {
        return null;
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
        long deviceId = 0; // FIX FIX FIX
        /*try {
            deviceId = getDataManager().getDeviceByImei(uniqueId).getId();
        } catch(Exception error) {
            Log.warning("Unknown device - " + uniqueId);
            return null;
        }*/
        buf.skipBytes(uniqueId.length());
        buf.skipBytes("&bin=".length());
        
        // Read header
        short packetId = buf.readUnsignedByte();
        buf.readUnsignedByte(); // dataOffset
        short packetCount = buf.readUnsignedByte();
        buf.readUnsignedByte(); // reserved
        short parameters = buf.readUnsignedByte(); // TODO: handle timezone
        
        // Send response
        if (channel != null) {
            sendContinue(channel);
            sendResponse(channel, packetId, packetCount);
        }
        
        // Parse data
        if (packetId == 0x31 || packetId == 0x32 || packetId == 0x36) {
            return parseFormatA(buf, deviceId);
        } else if (packetId == 0x34) {
            return parseFormatB(buf, deviceId);
        } //else if (0x38 0x4F 0x59)
        
        

        /*String sentence = (String) msg;

        // Detect device ID
        if (sentence.contains("$PGID")) {
            String imei = sentence.substring(6, sentence.length() - 3);
            try {
                deviceId = getDataManager().getDeviceByImei(imei).getId();
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei);
            }
        }

        // Parse message
        else if (sentence.contains("$GPRMC") && deviceId != null) {

            // Send response
            if (channel != null) {
                channel.write("OK1\r\n");
            }

            // Parse message
            Matcher parser = pattern.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            // Create new position
            Position position = new Position();
            position.setDeviceId(deviceId);

            Integer index = 1;

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            index += 1; // Skip milliseconds

            // Validity
            position.setValid(parser.group(index++).compareTo("A") == 0 ? true : false);

            // Latitude
            Double latitude = Double.valueOf(parser.group(index++));
            latitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
            position.setLatitude(latitude);

            // Longitude
            Double lonlitude = Double.valueOf(parser.group(index++));
            lonlitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("W") == 0) lonlitude = -lonlitude;
            position.setLongitude(lonlitude);

            // Speed
            String speed = parser.group(index++);
            if (speed != null) {
                position.setSpeed(Double.valueOf(speed));
            } else {
                position.setSpeed(0.0);
            }

            // Course
            String course = parser.group(index++);
            if (course != null) {
                position.setCourse(Double.valueOf(course));
            } else {
                position.setCourse(0.0);
            }

            // Date
            time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
            time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
            position.setTime(time.getTime());

            // Altitude
            position.setAltitude(0.0);

            return position;
        }*/

        return null;
    }

}
