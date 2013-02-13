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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class SyrusProtocolDecoder extends BaseProtocolDecoder {

    public SyrusProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    /**
     * Regular expressions pattern
     */
    //"REV691615354941+3570173+1397742703203212;ID=Test"
    static private Pattern pattern = Pattern.compile(
            "REV" +                        // Type
            "\\d{2}" +                     // Event index
            "(\\d{4})" +                   // Week
            "(\\d)" +                      // Day
            "(\\d{5})" +                   // Seconds
            "([\\+\\-]\\d{2})(\\d{5})" +      // Latitude
            "([\\+\\-]\\d{3})(\\d{5})" +      // Longitude
            "(\\d{3})" +                   // Speed
            "(\\d{3})" +                   // Course
            "\\d" +                        // Fix mode
            "(\\d)" +                      // Fix age
            ".*");

    private Date getTime(long week, long day, long seconds) {
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 1980);
        time.set(Calendar.MONTH, 0);
        time.set(Calendar.DAY_OF_MONTH, 6);

        long millis = time.getTimeInMillis();
        millis += ((week * 7 + day) * 24 * 60 * 60 + seconds) * 1000;

        return new Date(millis);
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;
        
        // Find message start
        int beginIndex = sentence.indexOf('>');
        if (beginIndex != -1) {
            sentence = sentence.substring(beginIndex + 1);
        }

        // Find device ID
        Long deviceId = null;
        beginIndex = sentence.indexOf(";ID=");
        if (beginIndex != -1) {
            beginIndex += 4;
            int endIndex = sentence.indexOf(';', beginIndex);
            if (endIndex == -1) {
                endIndex = sentence.length();
            }

            // Find device in database
            String id = sentence.substring(beginIndex, endIndex);
            try {
                deviceId = getDataManager().getDeviceByImei(id).getId();
            } catch(Exception error) {
                Log.warning("Unknown device - " + id);
                return null;
            }
            
            // Send response
            if (channel != null) {
                channel.write(id);
            }
        }

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>syrus</protocol>");
        position.setDeviceId(deviceId);

        Integer index = 1;

        // Time
        int week = Integer.valueOf(parser.group(index++));
        int day = Integer.valueOf(parser.group(index++));
        int seconds = Integer.valueOf(parser.group(index++));
        position.setTime(getTime(week, day, seconds));

        // Latitude
        String latitude = parser.group(index) + '.' + parser.group(index + 1);
        index += 2;
        position.setLatitude(Double.valueOf(latitude));

        // Latitude
        String longitude = parser.group(index) + '.' + parser.group(index + 1);
        index += 2;
        position.setLongitude(Double.valueOf(longitude));

        // Altitude
        position.setAltitude(0.0);
        
        // Speed and Course
        position.setSpeed(Double.valueOf(parser.group(index++)) * 0.868976);
        position.setCourse(Double.valueOf(parser.group(index++)));
        
        // Validity
        position.setValid(Integer.valueOf(parser.group(index++)) == 2);

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
