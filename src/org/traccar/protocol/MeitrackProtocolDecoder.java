/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class MeitrackProtocolDecoder extends BaseProtocolDecoder {

    public MeitrackProtocolDecoder(MeitrackProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$$").expression(".")          // flag
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("xxx,")                      // command
            .number("d+,").optional()
            .number("(d+),")                     // event
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time
            .number("([AV]),")                   // validity
            .number("(d+),")                     // satellites
            .number("(d+),")                     // gsm signal
            .number("(d+.?d*),")                 // speed
            .number("(d+),")                     // course
            .number("(d+.?d*),")                 // hdop
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // odometer
            .number("(d+),")                     // runtime
            .number("(d+)|")                     // mcc
            .number("(d+)|")                     // mnc
            .number("(x+)|")                     // lac
            .number("(x+),")                     // cell
            .number("(x+),")                     // state
            .number("(x+)?|")                    // adc1
            .number("(x+)?|")                    // adc2
            .number("(x+)?|")                    // adc3
            .number("(x+)|")                     // battery
            .number("(x+),")                     // power
            .groupBegin()
            .expression("([^,]+)?,")             // event specific
            .expression("[^,]*,")                // reserved
            .number("d*,")                       // protocol
            .number("(x{4})?")                   // fuel
            .groupEnd("?")
            .any()
            .text("*")
            .number("xx")
            .text("\r\n").optional()
            .compile();

    private Position decodeRegularMessage(Channel channel, ChannelBuffer buf) {

        Parser parser = new Parser(PATTERN, buf.toString(Charset.defaultCharset()));
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        int event = parser.nextInt();
        position.set(Event.KEY_EVENT, event);

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setValid(parser.next().equals("A"));

        position.set(Event.KEY_SATELLITES, parser.next());
        position.set(Event.KEY_GSM, parser.next());

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        position.set(Event.KEY_HDOP, parser.next());

        position.setAltitude(parser.nextDouble());

        position.set(Event.KEY_ODOMETER, parser.next());
        position.set("runtime", parser.next());
        position.set(Event.KEY_MCC, parser.nextInt());
        position.set(Event.KEY_MNC, parser.nextInt());
        position.set(Event.KEY_LAC, parser.next());
        position.set(Event.KEY_CID, parser.next());
        position.set(Event.KEY_STATUS, parser.next());

        for (int i = 1; i <= 3; i++) {
            if (parser.hasNext()) {
                position.set(Event.PREFIX_ADC + i, parser.nextInt(16));
            }
        }

        position.set(Event.KEY_BATTERY, parser.nextInt(16));
        position.set(Event.KEY_POWER, parser.nextInt(16));

        String eventData = parser.next();
        if (eventData != null && !eventData.isEmpty()) {
            switch (event) {
                case 37:
                    position.set(Event.KEY_RFID, eventData);
                    break;
                default:
                    position.set("event-data", eventData);
                    break;
            }
        }

        if (parser.hasNext()) {
            String fuel = parser.next();
            position.set(Event.KEY_FUEL,
                    Integer.parseInt(fuel.substring(0, 2), 16) + Integer.parseInt(fuel.substring(2), 16) * 0.01);
        }

        return position;
    }

    private List<Position> decodeBinaryMessage(Channel channel, ChannelBuffer buf) {
        List<Position> positions = new LinkedList<>();

        String flag = buf.toString(2, 1, Charset.defaultCharset());
        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');

        String imei = buf.toString(index + 1, 15, Charset.defaultCharset());
        if (!identify(imei, channel)) {
            return null;
        }

        buf.skipBytes(index + 1 + 15 + 1 + 3 + 1 + 2 + 2 + 4);

        while (buf.readableBytes() >= 0x34) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            position.set(Event.KEY_EVENT, buf.readUnsignedByte());

            position.setLatitude(buf.readInt() * 0.000001);
            position.setLongitude(buf.readInt() * 0.000001);

            position.setTime(new Date((946684800 + buf.readUnsignedInt()) * 1000)); // 946684800 = 2000-01-01

            position.setValid(buf.readUnsignedByte() == 1);

            position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());
            position.set(Event.KEY_GSM, buf.readUnsignedByte());

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());

            position.set(Event.KEY_HDOP, buf.readUnsignedShort() * 0.1);

            position.setAltitude(buf.readUnsignedShort());

            position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());
            position.set("runtime", buf.readUnsignedInt());
            position.set(Event.KEY_MCC, buf.readUnsignedShort());
            position.set(Event.KEY_MNC, buf.readUnsignedShort());
            position.set(Event.KEY_LAC, buf.readUnsignedShort());
            position.set(Event.KEY_CID, buf.readUnsignedShort());
            position.set(Event.KEY_STATUS, buf.readUnsignedShort());

            position.set(Event.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Event.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
            position.set(Event.KEY_POWER, buf.readUnsignedShort());

            buf.readUnsignedInt(); // geo-fence

            positions.add(position);
        }

        if (channel != null) {
            StringBuilder command = new StringBuilder("@@");
            command.append(flag).append(27 + positions.size() / 10).append(",");
            command.append(imei).append(",CCC,").append(positions.size()).append("*");
            int checksum = 0;
            for (int i = 0; i < command.length(); i += 1) {
                checksum += command.charAt(i);
            }
            command.append(String.format("%02x", checksum & 0xff).toUpperCase());
            command.append("\r\n");
            channel.write(command.toString()); // delete processed data
        }

        return positions;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        // Find type
        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');
        index = buf.indexOf(index + 1, buf.writerIndex(), (byte) ',');

        String type = buf.toString(index + 1, 3, Charset.defaultCharset());
        if (type.equals("CCC")) {
            return decodeBinaryMessage(channel, buf);
        } else {
            return decodeRegularMessage(channel, buf);
        }
    }

}
