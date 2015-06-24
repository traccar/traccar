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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TramigoProtocolDecoder extends BaseProtocolDecoder {

    public TramigoProtocolDecoder(TramigoProtocol protocol) {
        super(protocol);
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
        position.setProtocol(getProtocolName());
        position.set(Event.KEY_INDEX, index);
        position.setValid(true);

        // Get device id
        if (!identify(String.valueOf(id), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        if (protocol == 0x01 && (type == MSG_COMPACT || type == MSG_FULL)) {

            // TODO: send ack

            buf.readUnsignedShort(); // report trigger
            buf.readUnsignedShort(); // state flag

            position.setLatitude(buf.readUnsignedInt() * 0.0000001);
            position.setLongitude(buf.readUnsignedInt() * 0.0000001);

            buf.readUnsignedShort(); // GSM signal quality
            buf.readUnsignedShort(); // satellites in fix
            buf.readUnsignedShort(); // satellites in track
            buf.readUnsignedShort(); // GPS antenna state

            position.setSpeed(buf.readUnsignedShort() * 0.194384);
            position.setCourse((double )buf.readUnsignedShort());

            buf.readUnsignedInt(); // distance

            position.set(Event.KEY_BATTERY, buf.readUnsignedShort());

            buf.readUnsignedShort(); // battery charger status

            position.setTime(new Date(buf.readUnsignedInt() * 1000));

            // TODO: parse other data
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

            // Speed and Course
            pattern = Pattern.compile("([NSWE]{1,2}) with speed (\\d+) km/h");
            matcher = pattern.matcher(sentence);
            if (matcher.find()) {
                position.setSpeed(UnitsConverter.knotsFromKph(Double.valueOf(matcher.group(2))));
                position.setCourse(0); // matcher.group(1) for course
            }

            // Time
            pattern = Pattern.compile("(\\d{1,2}:\\d{2} \\w{3} \\d{1,2})");
            matcher = pattern.matcher(sentence);
            if (!matcher.find()) {
                return null;
            }
            DateFormat dateFormat = new SimpleDateFormat("HH:mm MMM d yyyy", Locale.ENGLISH);
            position.setTime(dateFormat.parse(matcher.group(1) + " " + Calendar.getInstance().get(Calendar.YEAR)));
            return position;
        }

        return null;
    }

}
