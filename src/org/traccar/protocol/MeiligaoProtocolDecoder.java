/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Crc;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class MeiligaoProtocolDecoder extends BaseProtocolDecoder {

    public MeiligaoProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    //"134743.003,A,0648.9866,S,10707.5795,E,000.0,000.0,260313"
    //,,*38|0.8|245|2000|03F6,0000,0000,001C,0000,0000,0000,0000|0194000201CC627C|1A|01160849yt
    
    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d+)," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "(\\d{2})(\\d{2}\\.\\d+)," +        // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d+)," +        // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+.\\d+)," +                    // Speed
            "(\\d+\\.?\\d*)?," +                // Course
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (DDMMYY)
            "(?:[^\\|]*\\|(\\d+\\.\\d+)\\|" +   // Dilution of precision
            "(\\d+\\.?\\d*)\\|)?" +             // Altitude
            "([0-9a-fA-F]+)?" +                 // State
            "(?:\\|([0-9a-fA-F]+),([0-9a-fA-F]+))?" + // ADC
            "(?:,([0-9a-fA-F]+),([0-9a-fA-F]+)" +
            ",([0-9a-fA-F]+),([0-9a-fA-F]+)" +
            ",([0-9a-fA-F]+),([0-9a-fA-F]+))?" +
            "(?:\\|([0-9a-fA-F]+))?" +          // Cell
            "(?:\\|([0-9a-fA-F]+))?" +          // Signal
            "(?:\\|([0-9a-fA-F]+))?" +          // Milage
            ".*"); // TODO: parse ADC

    /**
     * Parse device id
     */
    private String getId(ChannelBuffer buf) {
        String id = "";

        for (int i = 0; i < 7; i++) {
            int b = buf.getUnsignedByte(i);

            // First digit
            int d1 = (b & 0xf0) >> 4;
            if (d1 == 0xf) break;
            id += d1;

            // Second digit
            int d2 = (b & 0x0f);
            if (d2 == 0xf) break;
            id += d2;
        }

        return id;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        ChannelBuffer buf = (ChannelBuffer) msg;
        int command = buf.getUnsignedShort(7);

        // Login confirmation
        if (command == 0x5000) {
            ChannelBuffer sendBuf = HeapChannelBufferFactory.getInstance().getBuffer(18);
            sendBuf.writeByte('@');
            sendBuf.writeByte('@');
            sendBuf.writeShort(sendBuf.capacity());
            byte[] array = new byte[7];
            buf.getBytes(0, array);
            sendBuf.writeBytes(array);
            sendBuf.writeShort(0x4000);
            sendBuf.writeByte(0x01);
            sendBuf.writeShort(Crc.crc16X25Ccitt(sendBuf.toByteBuffer()));
            sendBuf.writeByte('\r');
            sendBuf.writeByte('\n');
            if (channel != null) {
                channel.write(sendBuf);
            }
            return null;
        }

        // Payload offset
        int offset = 7 + 2;

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>meiligao</protocol>");

        // Alarm
        if (command == 0x9999) {
            extendedInfo.append("<alarm>");
            extendedInfo.append(buf.getUnsignedByte(offset));
            extendedInfo.append("</alarm>");
        }

        // Data offset
        if (command == 0x9955) {
            offset += 0;
        } else if (command == 0x9016) {
            offset += 6;
        } else if (command == 0x9999) {
            offset += 1;
        } else {
            return null;
        }

        // Get device by id
        String id = getId(buf);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }

        // Parse message
        String sentence = buf.toString(offset, buf.readableBytes() - offset - 4, Charset.defaultCharset());
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            throw new ParseException(null, 0);
        }

        Integer index = 1;

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
        Double lonlitude = Double.valueOf(parser.group(index++));
        lonlitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) lonlitude = -lonlitude;
        position.setLongitude(lonlitude);

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
        String hdop = parser.group(index++);
        if (hdop != null) {
            extendedInfo.append("<hdop>");
            extendedInfo.append(hdop);
            extendedInfo.append("</hdop>");
        }

        // Altitude
        String altitude = parser.group(index++);
        if (altitude != null) {
            position.setAltitude(Double.valueOf(altitude));
        } else {
            position.setAltitude(0.0);
        }

        // State
        String state = parser.group(index++);
        if (state != null) {
            extendedInfo.append("<state>");
            extendedInfo.append(state);
            extendedInfo.append("</state>");
        }

        // ADC
        for (int i = 1; i <= 8; i++) {
            String adc = parser.group(index++);
            if (adc != null) {
                extendedInfo.append("<adc").append(i).append(">");
                extendedInfo.append(Integer.parseInt(adc, 16));
                extendedInfo.append("</adc").append(i).append(">");
            }
        }

        // Cell identifier
        String cell = parser.group(index++);
        if (cell != null) {
            extendedInfo.append("<cell>");
            extendedInfo.append(cell);
            extendedInfo.append("</cell>");
        }

        // GSM signal
        String gsm = parser.group(index++);
        if (gsm != null) {
            extendedInfo.append("<gsm>");
            extendedInfo.append(Integer.parseInt(gsm, 16));
            extendedInfo.append("</gsm>");
        }

        // Milage
        String milage = parser.group(index++);
        if (milage != null) {
            extendedInfo.append("<milage>");
            extendedInfo.append(Integer.parseInt(milage, 16));
            extendedInfo.append("</milage>");
        }

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
