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
 * AVL-08 tracker protocol decoder
 */
public class Avl08ProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public Avl08ProtocolDecoder(DataManager dataManager) {
        super(dataManager);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\$\\$.{2}" +               // Length
            "(\\d{15})\\|" +             // IMEI
            "(.{2})" +                   // Alarm Type
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2}).(\\d{3})," + // Time (HHMMSS.SSS)
            "([AV])," +                  // Validity
            "(\\d{2})(\\d{2}.\\d{4})," + // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}.\\d{4})," + // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+.\\d{2})?," +          // Speed
            "(\\d+.\\d{2})?," +          // Course
            "(\\d{2})(\\d{2})(\\d{2}),[^\\|]*\\|" + // Date (DDMMYY)
            "(\\d+.\\d)\\|(\\d+.\\d)\\|(\\d+.\\d)\\|" + // Dilution of precision
            "(\\d{12})\\|" +             // Status
            "(\\d{14})\\|" +             // Clock
            "(\\d{8})\\|" +              // Voltage
            "(\\d{8})\\|" +              // ADC
            "(.{8})\\|" +                // Cell
            "(.\\d{3})\\|" +             // Temperature
            "(\\d+.\\d{4})\\|" +         // Mileage
            "(\\d{4})\\|" +              // Serial
            "(.{10})?\\|?" +             // RFID
            ".+"); // TODO: Use non-capturing group

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        // Parse message
        String sentence = (String) msg;
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>avl08</protocol>");

        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());

        // Alarm type
        extendedInfo.append("<alarm>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</alarm>");

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

        // Altitude
        position.setAltitude(0.0);

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

        // Dilution of precision
        extendedInfo.append("<pdop>");
        extendedInfo.append(parser.group(index++).replaceFirst ("^0*(?![\\.$])", ""));
        extendedInfo.append("</pdop>");
        extendedInfo.append("<hdop>");
        extendedInfo.append(parser.group(index++).replaceFirst ("^0*(?![\\.$])", ""));
        extendedInfo.append("</hdop>");
        extendedInfo.append("<vdop>");
        extendedInfo.append(parser.group(index++).replaceFirst ("^0*(?![\\.$])", ""));
        extendedInfo.append("</vdop>");

        // Status
        extendedInfo.append("<status>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</status>");

        // Real time clock
        extendedInfo.append("<clock>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</clock>");

        // Voltage
        String voltage = parser.group(index++);
        position.setPower(Double.valueOf(voltage.substring(1, 4)) / 100);
        extendedInfo.append("<voltage>");
        extendedInfo.append(voltage);
        extendedInfo.append("</voltage>");

        // ADC
        extendedInfo.append("<adc>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</adc>");

        // Cell
        extendedInfo.append("<cell>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</cell>");

        // Temperature
        extendedInfo.append("<temperature>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</temperature>");

        // Mileage
        extendedInfo.append("<mileage>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</mileage>");

        // Serial
        extendedInfo.append("<serial>");
        extendedInfo.append(parser.group(index++).replaceFirst ("^0*", ""));
        extendedInfo.append("</serial>");

        // RFID
        String rfid = parser.group(index++);
        if (rfid != null) {
            extendedInfo.append("<rfid>");
            extendedInfo.append(rfid);
            extendedInfo.append("</rfid>");
        }

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
