/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class PiligrimProtocolDecoder extends BaseProtocolDecoder {
    
    public PiligrimProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    private void sendResponse(Channel channel, String message) {
        HttpResponse response = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setContent(ChannelBuffers.copiedBuffer(
                ByteOrder.BIG_ENDIAN, message, Charset.defaultCharset()));
        channel.write(response);
    }

    /*private List<Position> parseFormatA(ChannelBuffer buf, long deviceId) {
        List<Position> positions = new LinkedList<Position>();
        
        FloatReader latitudeReader = new FloatReader();
        FloatReader longitudeReader = new FloatReader();
        TimeReader timeReader = new TimeReader();
        
        try {
            while (buf.readable()) {
                Position position = new Position();
                position.setDeviceId(deviceId);
                ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("mta6");

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
                    extendedInfo.set("milage", buf.readUnsignedShort());
                }

                if (checkBit(flags, 4)) {
                    extendedInfo.set("fuel1", buf.readUnsignedInt());
                    extendedInfo.set("fuel2", buf.readUnsignedInt());
                    extendedInfo.set("hours1", buf.readUnsignedShort());
                    extendedInfo.set("hours2", buf.readUnsignedShort());
                }

                if (checkBit(flags, 5)) {
                    extendedInfo.set("adc1", buf.readUnsignedShort() & 0x03ff);
                    extendedInfo.set("adc2", buf.readUnsignedShort() & 0x03ff);
                    extendedInfo.set("adc3", buf.readUnsignedShort() & 0x03ff);
                    extendedInfo.set("adc4", buf.readUnsignedShort() & 0x03ff);
                }

                if (checkBit(flags, 6)) {
                    extendedInfo.set("temperature", buf.readByte());
                    buf.getUnsignedByte(buf.readerIndex()); // control (>> 4)
                    extendedInfo.set("sensor", buf.readUnsignedShort() & 0x0fff);
                    buf.readUnsignedShort(); // old sensor state (& 0x0fff)
                }

                if (checkBit(flags, 7)) {
                    extendedInfo.set("battery", buf.getUnsignedByte(buf.readerIndex()) >> 2);
                    extendedInfo.set("power", buf.readUnsignedShort() & 0x03ff);
                    buf.readByte(); // microcontroller temperature

                    extendedInfo.set("gsm", (buf.getUnsignedByte(buf.readerIndex()) >> 4) & 0x07);

                    int satellites = buf.readUnsignedByte() & 0x0f;
                    position.setValid(satellites >= 3);
                    extendedInfo.set("satellites", satellites);
                }

                position.setExtendedInfo(extendedInfo.toString());
                positions.add(position);
            }
        } catch (IndexOutOfBoundsException error) {
        }
        
        return positions;
    }*/

    private static final int MSG_GPS = 0xF1;
    private static final int MSG_GPS_SENSORS = 0xF2;
    private static final int MSG_EVENTS = 0xF3;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        HttpRequest request = (HttpRequest) msg;
        String uri = request.getUri();
        
        if (uri.startsWith("/config")) {

            sendResponse(channel, "CONFIG: OK");
        
        } else if (uri.startsWith("/addlog")) {

            sendResponse(channel, "ADDLOG: OK");
        
        } else if (uri.startsWith("/inform")) {

            sendResponse(channel, "INFORM: OK");
        
        } else if (uri.startsWith("/bingps")) {

            sendResponse(channel, "BINGPS: OK");
            
            // Identification
            long deviceId = 0;/*
            QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
            String imei = decoder.getParameters().get("imei").get(0);
            try {
                deviceId = getDataManager().getDeviceByImei(imei).getId();
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei);
                return null;
            }*/

            List<Position> positions = new LinkedList<Position>();
            ChannelBuffer buf = request.getContent();
            
            while (buf.readableBytes() > 2) {

                buf.readUnsignedByte(); // header
                int type = buf.readUnsignedByte();
                buf.readUnsignedShort(); // length
                
                if (type == MSG_GPS || type == MSG_GPS_SENSORS) {
                    
                    Position position = new Position();
                    ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("piligrim");
                    position.setDeviceId(deviceId);
                    
                    // Time
                    Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    time.clear();
                    time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
                    time.set(Calendar.MONTH, buf.getByte(buf.readerIndex()) & 0x0f);
                    time.set(Calendar.YEAR, 2000 + (buf.readUnsignedByte() >> 4));
                    time.set(Calendar.HOUR, buf.readUnsignedByte());
                    time.set(Calendar.MINUTE, buf.readUnsignedByte());
                    time.set(Calendar.SECOND, buf.readUnsignedByte());
                    position.setTime(time.getTime());
                    
                    // Latitude
                    double latitude = buf.readUnsignedByte();
                    latitude += buf.readUnsignedByte() / 60.0;
                    latitude += buf.readUnsignedByte() / 6000.0;
                    latitude += buf.readUnsignedByte() / 600000.0;
                    
                    // Longitude
                    double longitude = buf.readUnsignedByte();
                    longitude += buf.readUnsignedByte() / 60.0;
                    longitude += buf.readUnsignedByte() / 6000.0;
                    longitude += buf.readUnsignedByte() / 600000.0;
                    
                    // Hemisphere
                    int flags = buf.readUnsignedByte();
                    if ((flags & 0x01) != 0) latitude = -latitude;
                    if ((flags & 0x02) != 0) longitude = -longitude;
                    position.setLatitude(latitude);
                    position.setLongitude(longitude);
                    position.setAltitude(0.0);
                    
                    // Satellites
                    extendedInfo.set("satellites", buf.readUnsignedByte());
                    
                    // Speed
                    position.setSpeed((double) buf.readUnsignedByte());
                    
                    // Course
                    double course = buf.readUnsignedByte() << 1;
                    course += (flags >> 2) & 1;
                    course += buf.readUnsignedByte() / 100.0;
                    position.setCourse(course);

                    // Sensors
                    if (type == MSG_GPS_SENSORS) {

                        // External power
                        double power = buf.readUnsignedByte();
                        power += buf.readUnsignedByte() << 8;
                        extendedInfo.set("power", power / 100);

                        // Battery
                        double battery = buf.readUnsignedByte();
                        battery += buf.readUnsignedByte() << 8;
                        extendedInfo.set("battery", battery / 100);
                        
                        buf.skipBytes(6);
                        
                    }
                    
                    position.setExtendedInfo(extendedInfo.toString());
                    positions.add(position);
                    
                } else if (type == MSG_EVENTS) {
                    
                    buf.skipBytes(13);
                    
                }
                
            }
            
            return positions;
        }

        return null;
    }

}
