/*
 * Copyright 2014 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TramigoProtocolDecoder extends BaseProtocolDecoder {

    public TramigoProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    private static final int MSG_COMPACT = 0x0100;
    private static final int MSG_FULL = 0x00FE;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        int protocol = buf.readUnsignedByte();
        buf.readUnsignedByte(); // version id
        int index = buf.readUnsignedShort();
        int type = buf.readUnsignedShort();
        buf.readUnsignedShort(); // length
        buf.readUnsignedShort(); // mask
        buf.readUnsignedShort(); // checksum
        long id = buf.readUnsignedInt();
        buf.readUnsignedInt(); // time

        // Create new position
        Position position = new Position();
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
        extendedInfo.set("index", index);
        position.setValid(true);

        // Get device id
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(String.valueOf(id)).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }

        if (protocol == 0x01 && (type == MSG_COMPACT || type == MSG_FULL)) {

            // TODO: send ack

            buf.readUnsignedShort(); // report trigger
            buf.readUnsignedShort(); // state flag

            position.setLatitude(buf.readUnsignedInt() * 0.0000001);
            position.setLongitude(buf.readUnsignedInt() * 0.0000001);
            position.setAltitude(0.0);

            buf.readUnsignedShort(); // GSM signal quality
            buf.readUnsignedShort(); // satellites in fix
            buf.readUnsignedShort(); // satellites in track
            buf.readUnsignedShort(); // GPS antenna state

            position.setSpeed(buf.readUnsignedShort() * 0.194384);
            position.setCourse((double )buf.readUnsignedShort());

            buf.readUnsignedInt(); // distance

            extendedInfo.set("battery", buf.readUnsignedShort());

            buf.readUnsignedShort(); // battery charger status

            position.setTime(new Date(buf.readUnsignedInt() * 1000));

            // TODO: parse other data

            position.setExtendedInfo(extendedInfo.toString());
            return position;

        } else if (protocol == 0x80) {

            if (channel != null) {
                channel.write(ChannelBuffers.copiedBuffer("gprs,ack," + index, Charset.defaultCharset()));
            }

            String sentence = buf.toString(Charset.defaultCharset());

            // Coordinates
            Pattern pattern = Pattern.compile("(-?\\d+\\.\\d+), (-?\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(sentence);
            if (!matcher.find()) {
                return null;
            }
            position.setLatitude(Double.valueOf(matcher.group(1)));
            position.setLongitude(Double.valueOf(matcher.group(2)));
            position.setAltitude(0.0);

            // Speed and Course
            pattern = Pattern.compile("([NSWE]{1,2}) with speed (\\d+) km/h");
            matcher = pattern.matcher(sentence);
            if (matcher.find()) {
                position.setSpeed(Double.valueOf(matcher.group(2)) * 0.539957);
                position.setCourse(0.0); // matcher.group(1) for course
            } else {
                position.setSpeed(0.0);
                position.setCourse(0.0);
            }

            // Time
            pattern = Pattern.compile("(\\d{1,2}:\\d{2} \\w{3} \\d{1,2})");
            matcher = pattern.matcher(sentence);
            if (!matcher.find()) {
                return null;
            }
            DateFormat dateFormat = new SimpleDateFormat("HH:mm MMM d yyyy", Locale.ENGLISH);
            position.setTime(dateFormat.parse(matcher.group(1) + " " + Calendar.getInstance().get(Calendar.YEAR)));

            position.setExtendedInfo(extendedInfo.toString());
            return position;

        }

        return null;
    }

}
