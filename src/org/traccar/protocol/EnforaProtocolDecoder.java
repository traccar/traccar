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

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.GenericProtocolDecoder;
import org.traccar.helper.Log;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * Enfora protocol decoder
 */
public class EnforaProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public EnforaProtocolDecoder(DataManager dataManager, Integer resetDelay) {
        super(dataManager, resetDelay);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2}).(\\d{2})," + // Time (HHMMSS.SS)
            "([AV])," +                  // Validity
            "(\\d{2})(\\d{2}.\\d{6})," + // Latitude (DDMM.MMMMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}.\\d{6})," + // Longitude (DDDMM.MMMMMM)
            "([EW])," +
            "(\\d+.\\d)?," +             // Speed
            "(\\d+.\\d)?," +             // Course
            "(\\d{2})(\\d{2})(\\d{2})," + // Date (DDMMYY)
            ".*[\r\n\u0000]*");

    public static final int IMEI_LENGTH = 15;

    // TODO: avoid copy-paste (from XexunFrameDecoder)
    private static Integer find(
            ChannelBuffer buf,
            Integer start,
            Integer length,
            String subString) {

        int index = start;
        boolean match;

        for (; index < length; index++) {
            match = true;

            for (int i = 0; i < subString.length(); i++) {
                char c = (char) buf.getByte(index + i);
                if (c != subString.charAt(i)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return index;
            }
        }

        return null;
    }

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        // Find IMEI (Modem ID)
        String imei = null;
        for (int first = -1, i = 0; i < buf.readableBytes(); i++) {
            if (!Character.isDigit((char) buf.getByte(i))) {
                first = i + 1;
            }

            // Found digit string
            if (i - first == IMEI_LENGTH - 1) {
                imei = buf.toString(first, IMEI_LENGTH, Charset.defaultCharset());
                break;
            }
        }

        // Write log
        if (imei == null) {
            Log.warning("Enfora decoder failed to find IMEI");
            return null;
        }

        // Find GPSMC string
        Integer start = find(buf, 0, buf.readableBytes(), "GPRMC");
        if (start == null) {
            // Message does not contain GPS data
            return null;
        }
        String sentence = buf.toString(start, buf.readableBytes() - start, Charset.defaultCharset());

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        Integer index = 1;

        // Get device by IMEI
        position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)) * 10);

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

        return position;
    }

}
