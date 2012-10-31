/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.GenericProtocolDecoder;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * V680 tracker protocol decoder
 */
public class V680ProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public V680ProtocolDecoder(DataManager dataManager) {
        super(dataManager);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "#" +
            "(\\d+)#" +                    // IMEI
            "([^#]+)#" +                   // User
            "([01])#" +                    // Fix
            "([^#]+)#" +                   // Password
            "[^#]+#" +
            "(\\d+)#" +                    // Packet number
            "([^#]+)#" +                   // GSM base station
            "(\\d{3})(\\d{2}\\.\\d{4})," + // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d{2})(\\d{2}\\.\\d{4})," + // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d+\\.\\d+)," +             // Speed
            "(\\d+)#" +                    // Course
            "(\\d{2})(\\d{2})(\\d{2})#" +  // Date (DDMMYY)
            "(\\d{2})(\\d{2})(\\d{2})");   // Time (HHMMSS)

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>V680</protocol>");
        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());

        // User
        extendedInfo.append("<user>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</user>");

        // Validity
        position.setValid(parser.group(index++).compareTo("1") == 0 ? true : false);
        
        // Password
        extendedInfo.append("<password>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</password>");
        
        // Packet number
        extendedInfo.append("<packet>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</packet>");
        
        // GSM base station
        extendedInfo.append("<gsm>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</gsm>");
        
        // Longitude
        Double lonlitude = Double.valueOf(parser.group(index++));
        lonlitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) lonlitude = -lonlitude;
        position.setLongitude(lonlitude);
        
        // Latitude
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);
        
        // Altitude
        position.setAltitude(0.0);

        // Speed and Course
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(Double.valueOf(parser.group(index++)));
        
        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));

        // Time
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
