/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.Crc;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class CityeasyProtocolDecoder extends BaseProtocolDecoder {

    public CityeasyProtocolDecoder(CityeasyProtocol protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "(\\d{4})(\\d{2})(\\d{2})" +        // Date
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time
            "([AV])," +                         // Validity
            "(\\d+)," +                         // Satellites
            "([NS]),(\\d+\\.\\d+)," +           // Latitude
            "([EW]),(\\d+\\.\\d+)," +           // Longitude
            "(\\d+\\.\\d)," +                   // Speed
            "(\\d+\\.\\d)," +                   // HDOP
            "(\\d+\\.\\d);" +                   // Altitude
            "(\\d+)," +                         // MCC
            "(\\d+)," +                         // MNC
            "(\\d+)," +                         // LAC
            "(\\d+)" +                          // Cell
            ".*");

    private static final int MSG_LOCATION_REPORT = 0x0003;
    private static final int MSG_LOCATION_INQUIRY_RESPONSE = 0x0004;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShort(); // length

        String imei = ChannelBufferTools.readHexString(buf, 14);
        if (!identify(imei, channel, null, false)) {
            if (!identify(imei + Crc.luhnChecksum(Long.valueOf(imei)), channel)) {
                return null;
            }
        }

        int type = buf.readUnsignedShort();

        if (type == MSG_LOCATION_REPORT || type == MSG_LOCATION_INQUIRY_RESPONSE) {

            String sentence = buf.toString(buf.readerIndex(), buf.readableBytes() - 8, Charset.defaultCharset());
            Matcher parser = pattern.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            Integer index = 1;

            // Date and time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
            time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
            position.setTime(time.getTime());

            position.setValid(parser.group(index++).equals("A"));
            position.set(Event.KEY_SATELLITES, parser.group(index++));

            // Latitude
            String hemisphere = parser.group(index++);
            double latitude = Double.parseDouble(parser.group(index++));
            if (hemisphere.compareTo("S") == 0) {
                latitude = -latitude;
            }
            position.setLatitude(latitude);

            // Longitude
            hemisphere = parser.group(index++);
            double longitude = Double.parseDouble(parser.group(index++));
            if (hemisphere.compareTo("W") == 0) {
                longitude = -longitude;
            }
            position.setLongitude(longitude);

            position.setSpeed(Double.parseDouble(parser.group(index++)));
            position.set(Event.KEY_HDOP, Double.parseDouble(parser.group(index++)));
            position.setAltitude(Double.parseDouble(parser.group(index++)));

            position.set(Event.KEY_MCC, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_MNC, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_LAC, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_CELL, Integer.parseInt(parser.group(index++)));

            return position;
        }

        return null;
    }

}
