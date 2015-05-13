/*
 * Copyright 2013 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class SyrusProtocolDecoder extends BaseProtocolDecoder {
    
    boolean sendResponse;

    public SyrusProtocolDecoder(String protocol, boolean sendResponse) {
        super(protocol);
        this.sendResponse = sendResponse;
    }

    private static final Pattern pattern = Pattern.compile(
            "R[EP]V" +                     // Type
            "(?:\\d{2}" +                  // Event index
            "(\\d{4})" +                   // Week
            "(\\d))?" +                    // Day
            "(\\d{5})" +                   // Seconds
            "([\\+\\-]\\d{2})(\\d{5})" +   // Latitude
            "([\\+\\-]\\d{3})(\\d{5})" +   // Longitude
            "(\\d{3})" +                   // Speed
            "(\\d{3})" +                   // Course
            "\\d" +                        // Fix mode
            "(\\d)" +                      // Fix age
            ".*\r?\n?");

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
    
    private Date getTime(long seconds) {
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.set(Calendar.HOUR_OF_DAY, 0);
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        
        long millis = time.getTimeInMillis() + seconds * 1000;
        
        long diff = new Date().getTime() - millis;
        
        if (diff > 12 * 60 * 60 * 1000) {
            millis += 24 * 60 * 60 * 1000;
        } else if (diff < -12 * 60 * 60 * 1000) {
            millis -= 24 * 60 * 60 * 1000;
        }

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
        beginIndex = sentence.indexOf(";ID=");
        if (beginIndex != -1) {
            beginIndex += 4;
            int endIndex = sentence.indexOf(';', beginIndex);
            if (endIndex == -1) {
                endIndex = sentence.length();
            }

            // Find device in database
            String id = sentence.substring(beginIndex, endIndex);
            if (!identify(id)) {
                return null;
            }

            // Send response
            if (sendResponse && channel != null) {
                channel.write(id);
            }
        } else {
            return null;
        }

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocol());
        position.setDeviceId(getDeviceId());

        Integer index = 1;
        
        // Time
        String week = parser.group(index++);
        String day = parser.group(index++);
        int seconds = Integer.valueOf(parser.group(index++));
        if (week != null && day != null) {
            position.setTime(getTime(Integer.valueOf(week), Integer.valueOf(day), seconds));
        } else {
            position.setTime(getTime(seconds));
        }

        // Latitude
        String latitude = parser.group(index) + '.' + parser.group(index + 1);
        index += 2;
        position.setLatitude(Double.valueOf(latitude));

        // Latitude
        String longitude = parser.group(index) + '.' + parser.group(index + 1);
        index += 2;
        position.setLongitude(Double.valueOf(longitude));
        
        // Speed and Course
        position.setSpeed(UnitsConverter.knotsFromMph(Double.valueOf(parser.group(index++))));
        position.setCourse(Double.valueOf(parser.group(index++)));
        
        // Validity
        position.setValid(Integer.valueOf(parser.group(index++)) == 2);
        return position;
    }

}
