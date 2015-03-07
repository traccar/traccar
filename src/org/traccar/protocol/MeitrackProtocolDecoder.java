/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class MeitrackProtocolDecoder extends BaseProtocolDecoder {

    public MeitrackProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    private static final Pattern pattern = Pattern.compile(
            "\\$\\$." +                         // Flag
            "\\d+," +                           // Length
            "(\\d+)," +                         // IMEI
            "\\p{XDigit}{3}," +                 // Command
            "(?:\\d+,)?" +
            "(\\d+)," +                         // Event
            "(-?\\d+\\.\\d+)," +                // Latitude
            "(-?\\d+\\.\\d+)," +                // Longitude
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (YYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time (HHMMSS)
            "([AV])," +                         // Validity
            "(\\d+)," +                         // Satellites
            "(\\d+)," +                         // GSM Signal
            "(\\d+\\.?\\d*)," +                 // Speed
            "(\\d+)," +                         // Course
            "(\\d+\\.?\\d*)," +                 // HDOP
            "(-?\\d+)," +                       // Altitude
            "(\\d+)," +                         // Milage
            "(\\d+)," +                         // Runtime
            "(\\d+\\|\\d+\\|\\p{XDigit}+\\|\\p{XDigit}+)," + // Cell
            "(\\p{XDigit}+)," +                 // State
            "(\\p{XDigit}+)?\\|" +              // ADC1
            "(\\p{XDigit}+)?\\|" +              // ADC2
            "(\\p{XDigit}+)?\\|" +              // ADC3
            "(\\p{XDigit}+)\\|" +               // Battery
            "(\\p{XDigit}+)," +                 // Power
            ".*(\r\n)?");

    private Position decodeRegularMessage(Channel channel, ChannelBuffer buf) {

        // Parse message
        String sentence = buf.toString(Charset.defaultCharset());
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());

        Integer index = 1;

        // Identification
        String imei = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }

        // Event
        extendedInfo.set("event", parser.group(index++));

        // Coordinates
        position.setLatitude(Double.valueOf(parser.group(index++)));
        position.setLongitude(Double.valueOf(parser.group(index++)));

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        // Satellites
        extendedInfo.set("satellites", parser.group(index++));

        // GSM Signal
        extendedInfo.set("gsm", parser.group(index++));

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)) * 0.539957);

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));

        // HDOP
        extendedInfo.set("hdop", parser.group(index++));

        // Altitude
        position.setAltitude(Double.valueOf(parser.group(index++)));

        // Other
        extendedInfo.set("milage", parser.group(index++));
        extendedInfo.set("runtime", parser.group(index++));
        extendedInfo.set("cell", parser.group(index++));
        extendedInfo.set("state", parser.group(index++));
        
        // ADC
        String adc1 = parser.group(index++);
        if (adc1 != null) {
            extendedInfo.set("adc1", Integer.parseInt(adc1, 16));
        }
        String adc2 = parser.group(index++);
        if (adc2 != null) {
            extendedInfo.set("adc2", Integer.parseInt(adc2, 16));
        }
        String adc3 = parser.group(index++);
        if (adc3 != null) {
            extendedInfo.set("adc3", Integer.parseInt(adc3, 16));
        }
        extendedInfo.set("battery", Integer.parseInt(parser.group(index++), 16));
        extendedInfo.set("power", Integer.parseInt(parser.group(index++), 16));
        
        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

    private List<Position> decodeBinaryMessage(Channel channel, ChannelBuffer buf) {
        List<Position> positions = new LinkedList<Position>();
        
        String flag = buf.toString(2, 1, Charset.defaultCharset());
        int index = ChannelBufferTools.find(buf, 0, buf.readableBytes(), ",");
        
        // Identification
        long deviceId;
        String imei = buf.toString(index + 1, 15, Charset.defaultCharset());
        try {
            deviceId = getDataManager().getDeviceByImei(imei).getId();
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }
        
        buf.skipBytes(index + 1 + 15 + 1 + 3 + 1 + 2 + 2 + 4);
        
        while (buf.readableBytes() >= 0x34) {
            
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
            position.setDeviceId(deviceId);
            
            // Event
            extendedInfo.set("event", buf.readUnsignedByte());
            
            // Location
            position.setLatitude(buf.readInt() * 0.000001);
            position.setLongitude(buf.readInt() * 0.000001);
            
            // Time (946684800 - timestamp for 2000-01-01)
            position.setTime(new Date((946684800 + buf.readUnsignedInt()) * 1000));

            // Validity
            position.setValid(buf.readUnsignedByte() == 1);

            // Satellites
            extendedInfo.set("satellites", buf.readUnsignedByte());
            
            // GSM Signal
            extendedInfo.set("gsm", buf.readUnsignedByte());

            // Speed
            position.setSpeed(buf.readUnsignedShort() * 0.539957);

            // Course
            position.setCourse((double) buf.readUnsignedShort());

            // HDOP
            extendedInfo.set("hdop", buf.readUnsignedShort() * 0.1);

            // Altitude
            position.setAltitude((double) buf.readUnsignedShort());

            // Other
            extendedInfo.set("milage", buf.readUnsignedInt());
            extendedInfo.set("runtime", buf.readUnsignedInt());
            extendedInfo.set("cell",
                    buf.readUnsignedShort() + "|" + buf.readUnsignedShort() + "|" +
                    buf.readUnsignedShort() + "|" + buf.readUnsignedShort());
            extendedInfo.set("state", buf.readUnsignedShort());
        
            // ADC
            extendedInfo.set("adc1", buf.readUnsignedShort());
            extendedInfo.set("battery", buf.readUnsignedShort() * 0.01);
            extendedInfo.set("power", buf.readUnsignedShort());
            
            buf.readUnsignedInt(); // geo-fence
            
            position.setExtendedInfo(extendedInfo.toString());
            positions.add(position);
        }
        
        // Delete recorded data
        if (channel != null) {
            StringBuilder command = new StringBuilder("@@");
            command.append(flag).append(27 + positions.size() / 10).append(",");
            command.append(imei).append(",CCC,").append(positions.size()).append("*");
            int checksum = 0;
            for (int i = 0; i < command.length(); i += 1) checksum += command.charAt(i);
            command.append(String.format("%02x\r\n", checksum & 0xff).toUpperCase());
            channel.write(command.toString());
        }
        
        return positions;
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        ChannelBuffer buf = (ChannelBuffer) msg;
        
        // Find type
        Integer index = ChannelBufferTools.find(buf, 0, buf.readableBytes(), ",");
        index = ChannelBufferTools.find(buf, index + 1, buf.readableBytes(), ",");
        
        String type = buf.toString(index + 1, 3, Charset.defaultCharset());
        if (type.equals("CCC")) {
            return decodeBinaryMessage(channel, buf);
        } else {
            return decodeRegularMessage(channel, buf);
        }
    }

}
