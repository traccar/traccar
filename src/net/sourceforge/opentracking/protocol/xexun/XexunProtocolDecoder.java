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
package net.sourceforge.opentracking.protocol.xexun;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import net.sourceforge.opentracking.Position;
import net.sourceforge.opentracking.Device;
import net.sourceforge.opentracking.DataManager;
import java.sql.SQLException;

/**
 * Xexun tracker protocol decoder
 */
@ChannelPipelineCoverage("all")
public class XexunProtocolDecoder extends OneToOneDecoder {

    /**
     * Init device table
     */
    public XexunProtocolDecoder(DataManager dataManager) throws SQLException {

        // Create map
        setDevices(new HashMap());

        if (dataManager != null) {
            List deviceList = dataManager.readDevice();

            for (Object device: deviceList) {
                getDevices().put(((Device) device).getImei(), device);
            }
        }
    }

    /**
     * Devices
     */
    private Map devices;

    public Map getDevices() {
        return devices;
    }

    private void setDevices(Map newDevices) {
        devices = newDevices;
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "," +                               // Old: [\\d]+,
            "\\+?[\\d]+," +                     // Trusted phone number
            "GPRMC," +
            "([\\d]{2})([\\d]{2})([\\d]{2}).([\\d]{3})," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "([\\d]{2})([\\d]{2}.[\\d]{4})," +  // Latitude (DDMM.MMMM)
            "([NS])," +
            "([\\d]{3})([\\d]{2}.[\\d]{4})," +  // Longitude (DDDMM.MMMM)
            "([EW])," +
            "([\\d]+.[\\d]{2})," +              // Speed
            "([\\d]+.[\\d]{2})," +              // Course
            "([\\d]{2})([\\d]{2})([\\d]{2})," + // Date (DDMMYY)
            ".*imei:" +
            "([\\d]+)," +                       // IMEI
            ".*");

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws ParseException {

        // Parse message
        String sentence = (String) msg;
        System.out.println("parse: " + sentence);
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            throw new ParseException(null, 0);
        }

        // Create new position
        Position position = new Position();

        Integer index = 1;

        // Time
        Calendar time = new GregorianCalendar();
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

        // Speed and course
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(Double.valueOf(parser.group(index++)));

        // Date
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setDeviceId(((Device) getDevices().get(imei)).getId());

        return position;
    }

}
