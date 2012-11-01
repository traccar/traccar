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
import org.traccar.helper.Log;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * Gps 103 tracker protocol decoder
 */
public class Pt502ProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Initialize
     */
    public Pt502ProtocolDecoder(DataManager dataManager) {
        super(dataManager);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\$POS," +                         // Data Frame start
            "([\\d]+)," +                       // IMEI
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d{3})," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "([\\d]{2})([\\d]{2}.[\\d]{4})," +  // Latitude (DDMM.MMMM)
            "([NS])," +
            "([\\d]{3})([\\d]{2}.[\\d]{4})," +  // Longitude (DDDMM.MMMM)
            "([EW])," +
            "([\\d]+.[\\d]{2})," +              // Speed
            ".*");

    /*
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            Log.getLogger().info("BAD REGEX");
            return null;
        }else{
            Log.getLogger().info("GOOD REGEX");
        }

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>pt502</protocol>");

        Integer index = 1;

        // Get device by IMEI
        try {
            String imei = parser.group(index++);
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch (Exception e) {
            Log.getLogger().info(e.getMessage()+" LOG1");
        }
        
        // Time
        try {
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)));
            position.setTime(time.getTime());
        } catch (Exception e) {
            Log.getLogger().info(e.getMessage()+" LOG2");
        }

        // Validity
        try {
            position.setValid(parser.group(index++).compareTo("A") == 0 ? true : false);
        } catch (Exception e) {
            Log.getLogger().info(e.getMessage()+" LOG3");
        }

        // Latitude
        try {
            Double latitude = Double.valueOf(parser.group(index++));
            latitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("S") == 0) {
                latitude = -latitude;
            }
            position.setLatitude(latitude);
        } catch (Exception e) {
            Log.getLogger().info(e.getMessage()+" LOG4");
        }

        // Longitude
        try {
            Double lonlitude = Double.valueOf(parser.group(index++));
            lonlitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("W") == 0) {
                lonlitude = -lonlitude;
            }
            position.setLongitude(lonlitude);
        } catch (Exception e) {
            Log.getLogger().info(e.getMessage()+" LOG5");
        }

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
