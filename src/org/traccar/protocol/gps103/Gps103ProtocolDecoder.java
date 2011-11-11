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
package org.traccar.protocol.gps103;

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
public class Gps103ProtocolDecoder extends OneToOneDecoder {

    /**
     * Data manager
     */
    private DataManager dataManager;

    /**
     * Reset connection delay
     */
    private Integer resetDelay;

    /**
     * Init device table
     */
    public Gps103ProtocolDecoder(DataManager dataManager, Integer resetDelay) {
        this.dataManager = dataManager;
        this.resetDelay = resetDelay;
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "imei:" +
            "([\\d]+)," +                       // IMEI
            "[^,]+," +
            "(\\d{2})(\\d{2})(\\d{2})[\\d]+," + // Date
            "[+]?[\\d]*," +
            "[FL]," +                           // F - full / L - low
            "([\\d]{2})([\\d]{2})([\\d]{2}).([\\d]{3})," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "([\\d]{2})([\\d]{2}.[\\d]{4})," +  // Latitude (DDMM.MMMM)
            "([NS])," +
            "([\\d]{3})([\\d]{2}.[\\d]{4})," +  // Longitude (DDDMM.MMMM)
            "([EW])," +
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
        }
        
        // Send response #2
        if (sentence.length() == 15 && Character.isDigit(sentence.charAt(0))) {
            channel.write("ON");
        }

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();

        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setDeviceId(dataManager.getDeviceByImei(imei).getId());
        
        // Date
        Calendar time = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
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
        if (parser.group(index++).compareTo("W") == 0) lonlitude = -lonlitude;
        position.setLongitude(lonlitude);
        
        // Altitude
        position.setAltitude(0.0);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(0.0);

        return position;
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
