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
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.Log;
import org.traccar.model.Position;

/**
 * JT600 protocol decoder
 */
public class Jt600ProtocolDecoder extends BaseProtocolDecoder {

    /**
     * Initialize
     */
    public Jt600ProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    /**
     * Decode regular message
     */
    private Position decodeNormalMessage(ChannelBuffer buf) throws Exception {

        Position position = new Position();

        buf.readByte(); // header

        // Get device by identifier
        String id = Long.valueOf(ChannelBufferTools.readHexString(buf, 10)).toString();
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }

        buf.readByte(); // protocol version + data type

        buf.readBytes(2); // length

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, ChannelBufferTools.readHexInteger(buf, 2));
        time.set(Calendar.MONTH, ChannelBufferTools.readHexInteger(buf, 2) - 1);
        time.set(Calendar.YEAR, 2000 + ChannelBufferTools.readHexInteger(buf, 2));
        time.set(Calendar.HOUR, ChannelBufferTools.readHexInteger(buf, 2));
        time.set(Calendar.MINUTE, ChannelBufferTools.readHexInteger(buf, 2));
        time.set(Calendar.SECOND, ChannelBufferTools.readHexInteger(buf, 2));
        position.setTime(time.getTime());

        // Coordinates
        int temp = ChannelBufferTools.readHexInteger(buf, 8);
        double latitude = temp % 1000000;
        latitude /= 60 * 10000;
        latitude += temp / 1000000;
        temp = ChannelBufferTools.readHexInteger(buf, 9);
        double longitude = temp % 1000000;
        longitude /= 60 * 10000;
        longitude += temp / 1000000;

        // Flags
        byte flags = buf.readByte();
        position.setValid((flags & 0x1) == 0x1);
        if ((flags & 0x2) == 0) latitude = -latitude;
        position.setLatitude(latitude);
        if ((flags & 0x4) == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        double speed = ChannelBufferTools.readHexInteger(buf, 2);
        position.setSpeed(speed);

        // Course
        double course = buf.readUnsignedByte() * 2;
        position.setCourse(course);

        buf.readByte(); // number of satellites

        // Course
        double power = buf.readUnsignedByte();
        position.setPower(power);

        buf.readByte(); // other flags and sensors

        // Altitude
        double altitude = buf.readUnsignedShort();
        position.setAltitude(altitude);

        buf.readUnsignedShort(); // cell id
        buf.readUnsignedShort(); // lac

        buf.readUnsignedByte(); // gsm signal

        buf.readUnsignedByte(); // message number

        return position;
    }

    /**
     * Alert message pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\(" +
            "([\\d]+)," +                // Id
            "W01," +                     // Type
            "(\\d{3})(\\d{2}.\\d{4})," + // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d{2})(\\d{2}.\\d{4})," + // Latitude (DDMM.MMMM)
            "([NS])," +
            "([AV])," +                  // Validity
            "(\\d{2})(\\d{2})(\\d{2})," + // Date (DDMMYY)
            "(\\d{2})(\\d{2})(\\d{2})," + // Time (HHMMSS)
            "(\\d+)," +                  // Speed (km/h)
            "(\\d+)," +                  // Course
            "(\\d+)," +                  // Power
            "(\\d+)," +                  // GPS signal
            "(\\d+)," +                  // GSM signal
            "(\\d+)," +                  // Alert Type
            ".*\\)");

    /**
     * Decode alert message
     */
    private Position decodeAlertMessage(ChannelBuffer buf) throws Exception {

        String message = buf.toString(Charset.defaultCharset());

        // Parse message
        Matcher parser = pattern.matcher(message);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        Integer index = 1;

        // Get device by identifier
        String id = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }

        // Longitude
        Double lonlitude = Double.valueOf(parser.group(index++));
        lonlitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) lonlitude = -lonlitude;
        position.setLongitude(lonlitude);

        // Latitude
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0 ? true : false);

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));

        // Power
        position.setPower(Double.valueOf(parser.group(index++)));

        return position;
    }

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        char first = (char) buf.getByte(0);

        // Check message type
        if (first == '$') {
            return decodeNormalMessage(buf);
        } else if (first == '(') {
            return decodeAlertMessage(buf);
        }

        return null;
    }

}
