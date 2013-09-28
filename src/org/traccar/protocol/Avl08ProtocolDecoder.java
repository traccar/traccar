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
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

/**
 * AVL-08 tracker protocol decoder
 */
public class Avl08ProtocolDecoder extends BaseProtocolDecoder {

    /**
     * Initialize
     */
    public Avl08ProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\$\\$.{2}" +               // Length
            "(\\d+)\\|" +                // IMEI
            "(.{2})" +                   // Alarm Type
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d+)," + // Time (HHMMSS.SSS)
            "([AV])," +                  // Validity
            "(\\d{2})(\\d{2}\\.\\d+)," + // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d+)," + // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+\\.\\d+)?," +          // Speed
            "(\\d+\\.\\d+)?," +          // Course
            "(\\d{2})(\\d{2})(\\d{2}),[^\\|]*\\|" + // Date (DDMMYY)
            "(\\d+\\.?\\d+)\\|(\\d+\\.?\\d+)\\|(\\d+\\.?\\d+)\\|" + // Dilution of precision
            "(\\d{12})\\|" +             // Status
            "(\\d{14})\\|" +             // Clock
            "(\\d{8})\\|" +              // Voltage
            "(\\d{8})\\|" +              // ADC
            "([0-9a-fA-F]{8})\\|" +      // Cell
            "(.\\d{3})\\|" +             // Temperature
            "(\\d+.\\d{4})\\|" +         // Mileage
            "(\\d{4})\\|" +              // Serial
            "(.{10})?\\|?" +             // RFID
            ".+"); // TODO: Use non-capturing group

    /**
     * Decode message
     */
    @Override
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
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("avl08");

        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }

        // Alarm type
        extendedInfo.set("alarm", parser.group(index++));

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
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

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
        extendedInfo.set("pdop", parser.group(index++).replaceFirst ("^0*(?![\\.$])", ""));
        extendedInfo.set("hdop", parser.group(index++).replaceFirst ("^0*(?![\\.$])", ""));
        extendedInfo.set("vdop", parser.group(index++).replaceFirst ("^0*(?![\\.$])", ""));

        // Status
        extendedInfo.set("status", parser.group(index++));

        // Real time clock
        extendedInfo.set("clock", parser.group(index++));

        // Voltage
        String voltage = parser.group(index++);
        extendedInfo.set("power", Double.valueOf(voltage.substring(1, 4)) / 100);
        extendedInfo.set("voltage", voltage);

        // ADC
        extendedInfo.set("adc", parser.group(index++));

        // Cell
        extendedInfo.set("cell", parser.group(index++));

        // Temperature
        extendedInfo.set("temperature", parser.group(index++));

        // Mileage
        extendedInfo.set("mileage", parser.group(index++));

        // Serial
        extendedInfo.set("serial", parser.group(index++).replaceFirst ("^0*", ""));

        // RFID
        extendedInfo.set("rfid", parser.group(index++));

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
