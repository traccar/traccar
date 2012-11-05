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
public class Gps103ProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public Gps103ProtocolDecoder(DataManager dataManager) {
        super(dataManager);
    }

    /**
     * Regular expressions pattern
     */
    //imei:863070010423167,tracker,1211051840,,F,104000.000,A,2220.6483,N,11407.6377,,0,0,
    static private Pattern pattern = Pattern.compile(
            "imei:" +
            "([\\d]+)," +                       // IMEI
            "([^,]+)," +                        // Alarm
            "(\\d{2})(\\d{2})(\\d{2})[\\d]+," + // Date
            "[^,]*," +
            "[FL]," +                           // F - full / L - low
            "([\\d]{2})([\\d]{2})([\\d]{2}).([\\d]{3})," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "([\\d]{2})([\\d]{2}.[\\d]{4})," +  // Latitude (DDMM.MMMM)
            "([NS])," +
            "([\\d]{3})([\\d]{2}.[\\d]{4})," +  // Longitude (DDDMM.MMMM)
            "([EW])?," +
            "([\\d]+.[\\d]{2})," +              // Speed
            ".*");

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Send response #1
        if (sentence.contains("##")) {
            channel.write("LOAD");
            return null;
        }

        // Send response #2
        if (sentence.length() == 15 && Character.isDigit(sentence.charAt(0))) {
            channel.write("ON");
            return null;
        }

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>gps103</protocol>");

        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());

        // Alarm message
        extendedInfo.append("<alarm>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</alarm>");

        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));

        // Time
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

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
        String hemisphere = parser.group(index++);
        if (hemisphere != null) {
            if (hemisphere.compareTo("W") == 0) lonlitude = -lonlitude;
        }
        position.setLongitude(lonlitude);

        // Altitude
        position.setAltitude(0.0);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(0.0);

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
