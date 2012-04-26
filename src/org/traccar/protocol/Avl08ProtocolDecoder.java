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
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.Position;
import org.traccar.DataManager;
import org.traccar.Device; // DELME
import org.traccar.GenericProtocolDecoder;

/**
 * AVL-08 tracker protocol decoder
 */
public class Avl08ProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public Avl08ProtocolDecoder(DataManager dataManager, Integer resetDelay) {
        super(dataManager, resetDelay);
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
        String extendedInfo = "<protocol>avl08</protocol>";

        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        
        // Alarm type
        extendedInfo += "<alarm>" + parser.group(index++) + "</alarm>";
        
        // Time
        Calendar time = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
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
        extendedInfo += "<pdop>" + parser.group(index++).replaceFirst ("^0*(?![\\.$])", "") + "</pdop>";
        extendedInfo += "<hdop>" + parser.group(index++).replaceFirst ("^0*(?![\\.$])", "") + "</hdop>";
        extendedInfo += "<vdop>" + parser.group(index++).replaceFirst ("^0*(?![\\.$])", "") + "</vdop>";

        // Status
        extendedInfo += "<status>" + parser.group(index++) + "</status>";

        // Real time clock
        extendedInfo += "<clock>" + parser.group(index++) + "</clock>";

        // Voltage
        String voltage = parser.group(index++);
        position.setPower(Double.valueOf(voltage.substring(1, 4)) / 100);
        extendedInfo += "<voltage>" + voltage + "</voltage>";

        // ADC
        extendedInfo += "<adc>" + parser.group(index++) + "</adc>";

        // Cell
        extendedInfo += "<cell>" + parser.group(index++) + "</cell>";

        // Temperature
        extendedInfo += "<temperature>" + parser.group(index++) + "</temperature>";

        // Mileage
        extendedInfo += "<mileage>" + parser.group(index++) + "</mileage>";

        // Serial
        extendedInfo += "<serial>" + parser.group(index++).replaceFirst ("^0*", "") + "</serial>";

        // RFID
        String rfid = parser.group(index++);
        if (rfid != null) {
            extendedInfo += "<rfid>" + rfid + "</rfid>";
        }

        // Extended info
        position.setExtendedInfo(extendedInfo);

        return position;
    }

}
