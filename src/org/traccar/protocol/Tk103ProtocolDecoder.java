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
 * Gps 103 tracker protocol decoder
 */
public class Tk103ProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public Tk103ProtocolDecoder(DataManager dataManager) {
        super(dataManager);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\(" +
            "(\\d+)" +                   // Device ID
            "(.{4})" +                   // Command
            "(\\d{15})" +                // IMEI (?)
            "(\\d{2})(\\d{2})(\\d{2})" + // Date (YYMMDD)
            "([AV])" +                   // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})" +  // Latitude (DDMM.MMMM)
            "([NS])" +
            "(\\d{3})(\\d{2}\\.\\d{4})" +  // Longitude (DDDMM.MMMM)
            "([EW])" +
            "(\\d+\\.\\d)" +             // Speed
            "(\\d{2})(\\d{2})(\\d{2})" + // Time (HHMMSS)
            "(\\d+\\.\\d{2})" +          // Course
            "(\\d+)" +                   // State
            ".+");                       // Mileage (?)

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;
        
        // TODO: Send answer
        //(090411121854AP05)

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();

        Integer index = 1;
        index += 2; // Skip Device ID and command

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());

        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));

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

        // Altitude
        position.setAltitude(0.0);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Time
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));

        return position;
    }

}
