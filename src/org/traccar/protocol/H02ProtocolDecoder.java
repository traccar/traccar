/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class H02ProtocolDecoder extends BaseProtocolDecoder {

    public H02ProtocolDecoder(String protocol) {
        super(protocol);
    }
    
    private static double readCoordinate(ChannelBuffer buf, boolean lon) {
        
        int degrees = ChannelBufferTools.readHexInteger(buf, 2);
        if (lon) {
            degrees = degrees * 10 + (buf.getUnsignedByte(buf.readerIndex()) >> 4);
        }
        
        double result = 0;
        if (lon) {
            result = buf.readUnsignedByte() & 0x0f;
        }
        result = result * 10 + ChannelBufferTools.readHexInteger(buf, lon ? 5 : 6) * 0.0001;
        
        result /= 60;
        result += degrees;

        return result;
    }
    
    private Position decodeBinary(ChannelBuffer buf) {
        
        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocol());
        
        buf.readByte(); // marker

        // Identification
        if (!identify(ChannelBufferTools.readHexString(buf, 10))) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR_OF_DAY, ChannelBufferTools.readHexInteger(buf, 2));
        time.set(Calendar.MINUTE, ChannelBufferTools.readHexInteger(buf, 2));
        time.set(Calendar.SECOND, ChannelBufferTools.readHexInteger(buf, 2));
        time.set(Calendar.DAY_OF_MONTH, ChannelBufferTools.readHexInteger(buf, 2));
        time.set(Calendar.MONTH, ChannelBufferTools.readHexInteger(buf, 2) - 1);
        time.set(Calendar.YEAR, 2000 + ChannelBufferTools.readHexInteger(buf, 2));
        position.setTime(time.getTime());
        
        // Location
        double latitude = readCoordinate(buf, false);
        int x = buf.readByte(); // reserved
        double longitude = readCoordinate(buf, true);
        int flags = buf.readUnsignedByte() & 0x0f;
        position.setValid((flags & 0x02) != 0);
        if ((flags & 0x04) == 0) latitude = -latitude;
        if ((flags & 0x08) == 0) longitude = -longitude;
        position.setLatitude(latitude);
        position.setLongitude(longitude);

        // Speed and course
        position.setSpeed(ChannelBufferTools.readHexInteger(buf, 3));
        position.setCourse((buf.readUnsignedByte() & 0x0f) * 100.0 + ChannelBufferTools.readHexInteger(buf, 2));
        
        // Status
        position.set(Event.KEY_STATUS, ChannelBufferTools.readHexString(buf, 8));
        return position;
    }

    private static final Pattern pattern = Pattern.compile(
    	    "\\*..," +                          // Manufacturer
            "(\\d+)," +                         // IMEI
            "V\\d," +                           // Version?
            ".*" +
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time (HHMMSS)
            "([AV])," +                         // Validity
            "-?(\\d+)-?(\\d{2}.\\d+)," +        // Latitude (DDMM.MMMM)
            "([NS])," +
            "-?(\\d+)-?(\\d{2}.\\d+)," +        // Longitude (DDMM.MMMM)
            "([EW])," +
            "(\\d+.?\\d*)," +                   // Speed
            "(\\d+.?\\d*)?," +                  // Course
            "(\\d{2})(\\d{2})(\\d{2})," +       // Date (DDMMYY)
            "(\\p{XDigit}{8})" +                // Status
            ".*");
    
    private Position decodeText(String sentence) {

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocol());

        Integer index = 1;

        // Get device by IMEI
        if (!identify(parser.group(index++))) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        // Latitude
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Course
        String course = parser.group(index++);
        if (course != null) {
            position.setCourse(Double.valueOf(course));
        }

        // Date
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());
        
        // Status
        position.set(Event.KEY_STATUS, parser.group(index++));
        return position;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        ChannelBuffer buf = (ChannelBuffer) msg;
        String marker = buf.toString(0, 1, Charset.defaultCharset());
        
        // TODO X mode?

        if (marker.equals("*")) {
            return decodeText(buf.toString(Charset.defaultCharset()));
        } else if (marker.equals("$")) {
            return decodeBinary(buf);
        }

        return null;
    }

}
