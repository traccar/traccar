/*
 * Copyright 2010 Anton Tananaev (anton@tananaev.com)
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
package org.traccar.protocol.t55;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.traccar.Position;
import org.traccar.DataManager;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;

/**
 * Gps 103 tracker protocol decoder
 */
public class T55ProtocolDecoder extends OneToOneDecoder {

    /**
     * Data manager
     */
    private DataManager dataManager;

    /**
     * Reset connection delay
     */
    private Integer resetDelay;
    
    /**
     * Device ID
     */
    private Long deviceId;

    /**
     * Init device table
     */
    public T55ProtocolDecoder(DataManager dataManager, Integer resetDelay) {
        this.dataManager = dataManager;
        this.resetDelay = resetDelay;
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2}).(\\d{3})," + // Time (HHMMSS.SSS)
            "([AV])," +                  // Validity
            "(\\d{2})(\\d{2}.\\d{4})," + // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}.\\d{4})," + // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+.\\d{2})?," +           // Speed
            "(\\d+.\\d{2})?," +           // Course
            "(\\d{2})(\\d{2})(\\d{2})" + // Date (DDMMYY)
            ".+");                     // Other (Checksumm)

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;
        
        //System.out.println(sentence);

        // Detect device ID
        if (sentence.contains("$PGID")) {
            String imei = sentence.substring(6, 6 + 15);
            deviceId = dataManager.getDeviceByImei(imei).getId();
        }

        // Parse message
        else if (sentence.contains("$GPRMC") && deviceId != null) {

            // Send response
            if (channel != null) {
                channel.write("OK1\r\n");
            }

            // Parse message
            Matcher parser = pattern.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }
            
            // Create new position
            Position position = new Position();
            position.setDeviceId(deviceId);
            
            Integer index = 1;

            // Time
            Calendar time = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            index += 1; // Skip milliseconds
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
            if (parser.group(index++).compareTo("W") == 0) lonlitude = -lonlitude;
            position.setLongitude(lonlitude);

            // Speed
            String speed = parser.group(index++);
            if (speed != null) {
                position.setSpeed(Double.valueOf(speed));
            } else {
                position.setSpeed(0.0);
            }

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
            
            // Altitude
            position.setAltitude(0.0);
            
            return position;
        }

        return null;
    }

    /**
     * Disconnect channel
     */
    class DisconnectTask extends TimerTask {
        private Channel channel;

        public DisconnectTask(Channel channel) {
            this.channel = channel;
        }

        public void run() {
            channel.disconnect();
        }
    }

    /**
     * Handle connect event
     */
    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        super.handleUpstream(ctx, evt);

        if (evt instanceof ChannelStateEvent) {
            ChannelStateEvent event = (ChannelStateEvent) evt;

            if (event.getState() == ChannelState.CONNECTED && event.getValue() != null && resetDelay != 0) {
                new Timer().schedule(new DisconnectTask(evt.getChannel()), resetDelay);
            }
        }
    }

}
