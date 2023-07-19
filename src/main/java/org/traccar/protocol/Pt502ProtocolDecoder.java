/*
 * Copyright 2012 - 2020 Anton Tananaev (anton@traccar.org)
 * Copyright 2012 Luis Parada (luis.parada@gmail.com)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Pt502ProtocolDecoder extends BaseProtocolDecoder {

    private static final int MAX_CHUNK_SIZE = 960;

    private ByteBuf photo;

    public Pt502ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .any().text("$")
            .expression("([^,]+),")              // type
            .number("(d+),")                     // id
            .number("(dd)(dd)(dd).(ddd),")       // time (hhmmss.sss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.dddd),")            // latitude
            .expression("([NS]),")
            .number("(d+)(dd.dddd),")            // longitude
            .expression("([EW]),")
            .number("(d+.d+)?,")                 // speed
            .number("(d+.d+)?,")                 // course
            .number("(dd)(dd)(dd),,,?")          // date (ddmmyy)
            .expression(".?/")
            .expression("([01])+,")              // input
            .expression("([01])+/")              // output
            .expression("([^/]+)?/")             // adc
            .number("(d+)")                      // odometer
            .expression("/([^/]+)?/")            // rfid
            .number("(xxx)").optional(2)         // state
            .any()
            .compile();

    private String decodeAlarm(String value) {
        switch (value) {
            case "IN1":
                return Position.ALARM_SOS;
            case "GOF":
                return Position.ALARM_GEOFENCE;
            case "TOW":
                return Position.ALARM_TOW;
            case "HDA":
                return Position.ALARM_ACCELERATION;
            case "HDB":
                return Position.ALARM_BRAKING;
            case "FDA":
                return Position.ALARM_FATIGUE_DRIVING;
            case "SKA":
                return Position.ALARM_VIBRATION;
            case "PMA":
                return Position.ALARM_MOVEMENT;
            case "CPA":
                return Position.ALARM_POWER_CUT;
            default:
                return null;
        }
    }

    private Position decodePosition(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.set(Position.KEY_ALARM, decodeAlarm(parser.next()));

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_INPUT, parser.next());
        position.set(Position.KEY_OUTPUT, parser.next());

        if (parser.hasNext()) {
            String[] values = parser.next().split(",");
            for (int i = 0; i < values.length; i++) {
                position.set(Position.PREFIX_ADC + (i + 1), Integer.parseInt(values[i], 16));
            }
        }

        position.set(Position.KEY_ODOMETER, parser.nextInt(0));
        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        if (parser.hasNext()) {
            int value = parser.nextHexInt(0);
            position.set(Position.KEY_BATTERY, value >> 8);
            position.set(Position.KEY_RSSI, (value >> 4) & 0xf);
            position.set(Position.KEY_SATELLITES, value & 0xf);
        }

        return position;
    }

    private void requestPhotoFragment(Channel channel) {
        if (channel != null) {
            int offset = photo.writerIndex();
            int size = Math.min(photo.writableBytes(), MAX_CHUNK_SIZE);
            channel.writeAndFlush(new NetworkMessage("#PHD" + offset + "," + size + "\r\n", channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int typeEndIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');
        String type = buf.toString(buf.readerIndex(), typeEndIndex - buf.readerIndex(), StandardCharsets.US_ASCII);

        if (type.startsWith("$PHD")) {

            int dataIndex = buf.indexOf(typeEndIndex + 1, buf.writerIndex(), (byte) ',') + 1;
            buf.readerIndex(dataIndex);

            if (photo != null) {

                photo.writeBytes(buf.readSlice(buf.readableBytes()));

                if (photo.writableBytes() > 0) {

                    requestPhotoFragment(channel);

                } else {

                    DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());

                    getLastLocation(position, null);

                    position.set(Position.KEY_IMAGE, writeMediaFile(deviceSession.getUniqueId(), photo, "jpg"));
                    photo.release();
                    photo = null;

                    return position;

                }

            }

        } else {

            if (type.startsWith("$PHO")) {
                int size = Integer.parseInt(type.split("-")[0].substring(4));
                if (size > 0) {
                    photo = Unpooled.buffer(size);
                    requestPhotoFragment(channel);
                }
            }

            return decodePosition(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));

        }

        return null;
    }

}
