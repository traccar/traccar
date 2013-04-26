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
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class Xt7ProtocolDecoder extends BaseProtocolDecoder {

    public Xt7ProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    static private Pattern pattern = Pattern.compile(
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d+)," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "(\\d{2})(\\d{2}\\.\\d+)," +        // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d+)," +        // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+.\\d+)," +                    // Speed
            "(\\d+\\.?\\d*)?," +                // Course
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (DDMMYY)
            "[^\\*]+\\*[0-9a-fA-F]{2}," +
            "(\\d+,\\d+)," +                    // IMSI
            "([0-9a-fA-F]+,[0-9a-fA-F]+)," +    // Cell
            "(\\d+)," +                         // Signal quality
            "(\\d+)," +                         // Battery
            "([01]{4})," +                      // Flags
            "([01]{4})," +                      // Sensors
            "(\\d+)," +                         // Fuel
            "(.+)?");                           // Alarm

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        ChannelBuffer buf = (ChannelBuffer) msg;
        
        buf.skipBytes(3); // STX

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>xt7</protocol>");
        
        // Get device by id
        String id = buf.readBytes(16).toString(Charset.defaultCharset()).trim();
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }
        
        buf.readUnsignedByte(); // command
        int length = buf.readUnsignedByte();
        
        // Parse message
        String sentence = buf.readBytes(length).toString(Charset.defaultCharset());
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }
        
        Integer index = 1;

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)));

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
        position.setSpeed(Double.valueOf(parser.group(index++)));

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

        // IMSI
        extendedInfo.append("<imsi>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</imsi>");

        // Cell
        extendedInfo.append("<cell>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</cell>");

        // GSM signal quality
        extendedInfo.append("<gsm>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</gsm>");
        
        // Battery
        position.setPower(Double.valueOf(parser.group(index++)));
        
        // Flags
        extendedInfo.append("<flags>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</flags>");

        // Sensors
        extendedInfo.append("<sensors>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</sensors>");

        // Fuel
        extendedInfo.append("<fuel>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</fuel>");

        // Alarm
        extendedInfo.append("<alarm>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</alarm>");

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
