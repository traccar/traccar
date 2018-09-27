/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class UproProtocolDecoder extends BaseProtocolDecoder {

    public UproProtocolDecoder(UproProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_HEADER = new PatternBuilder()
            .text("*")
            .expression("(..20)")                // head
            .expression("([01])")                // ack
            .number("(d+),")                     // device id
            .expression("(.)")                   // type
            .expression("(.)")                   // subtype
            .any()
            .compile();

    private static final Pattern PATTERN_LOCATION = new PatternBuilder()
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("(dd)(dd)(dddd)")            // latitude
            .number("(ddd)(dd)(dddd)")           // longitude
            .number("(d)")                       // flags
            .number("(dd)")                      // speed
            .number("(dd)")                      // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .compile();

    private void decodeLocation(Position position, String data) {
        Parser parser = new Parser(PATTERN_LOCATION, data);
        if (parser.matches()) {

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

            position.setValid(true);
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN));

            int flags = parser.nextInt(0);
            position.setValid(BitUtil.check(flags, 0));
            if (!BitUtil.check(flags, 1)) {
                position.setLatitude(-position.getLatitude());
            }
            if (!BitUtil.check(flags, 2)) {
                position.setLongitude(-position.getLongitude());
            }

            position.setSpeed(parser.nextInt(0) * 2);
            position.setCourse(parser.nextInt(0) * 10);

            dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
            position.setTime(dateBuilder.getDate());

        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getByte(buf.readerIndex()) != '*') {
            return null;
        }

        int headerIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '&');
        if (headerIndex < 0) {
            headerIndex = buf.writerIndex();
        }
        String header = buf.readSlice(headerIndex - buf.readerIndex()).toString(StandardCharsets.US_ASCII);

        Parser parser = new Parser(PATTERN_HEADER, header);
        if (!parser.matches()) {
            return null;
        }

        String head = parser.next();
        boolean reply = parser.next().equals("1");

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String type = parser.next();
        String subtype = parser.next();

        if (reply && channel != null) {
            channel.writeAndFlush(new NetworkMessage("*" + head + "Y" + type + subtype + "#", remoteAddress));
        }

        while (buf.isReadable()) {

            buf.readByte(); // skip delimiter

            byte dataType = buf.readByte();

            int delimiterIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '&');
            if (delimiterIndex < 0) {
                delimiterIndex = buf.writerIndex();
            }

            ByteBuf data = buf.readSlice(delimiterIndex - buf.readerIndex());

            switch (dataType) {
                case 'A':
                    decodeLocation(position, data.toString(StandardCharsets.US_ASCII));
                    break;
                case 'B':
                    position.set(Position.KEY_STATUS, data.toString(StandardCharsets.US_ASCII));
                    break;
                case 'C':
                    long odometer = 0;
                    while (data.isReadable()) {
                        odometer <<= 4;
                        odometer += data.readByte() - (byte) '0';
                    }
                    position.set(Position.KEY_ODOMETER, odometer * 2 * 1852 / 3600);
                    break;
                case 'F':
                    position.setSpeed(
                            Integer.parseInt(data.readSlice(4).toString(StandardCharsets.US_ASCII)) * 0.1);
                    break;
                case 'K':
                    position.set("statusExtended", data.toString(StandardCharsets.US_ASCII));
                    break;
                case 'P':
                    position.setNetwork(new Network(CellTower.from(
                            Integer.parseInt(data.readSlice(4).toString(StandardCharsets.US_ASCII)),
                            Integer.parseInt(data.readSlice(4).toString(StandardCharsets.US_ASCII)),
                            Integer.parseInt(data.readSlice(4).toString(StandardCharsets.US_ASCII), 16),
                            Integer.parseInt(data.readSlice(4).toString(StandardCharsets.US_ASCII), 16))));
                    break;
                case 'Q':
                    position.set("obdPid", ByteBufUtil.hexDump(data));
                    break;
                case 'R':
                    if (head.startsWith("HQ")) {
                        position.set(Position.KEY_RSSI,
                                Integer.parseInt(data.readSlice(2).toString(StandardCharsets.US_ASCII)));
                        position.set(Position.KEY_SATELLITES,
                                Integer.parseInt(data.readSlice(2).toString(StandardCharsets.US_ASCII)));
                    } else {
                        position.set("odbTravel", ByteBufUtil.hexDump(data));
                    }
                    break;
                case 'S':
                    position.set("obdTraffic", ByteBufUtil.hexDump(data));
                    break;
                case 'T':
                    position.set(Position.KEY_BATTERY_LEVEL,
                            Integer.parseInt(data.readSlice(2).toString(StandardCharsets.US_ASCII)));
                    break;
                case 'V':
                    position.set(Position.KEY_POWER,
                            Integer.parseInt(data.readSlice(4).toString(StandardCharsets.US_ASCII)) * 0.1);
                    break;
                default:
                    break;
            }

        }

        if (position.getLatitude() != 0 && position.getLongitude() != 0) {
            return position;
        }

        return null;
    }

}
