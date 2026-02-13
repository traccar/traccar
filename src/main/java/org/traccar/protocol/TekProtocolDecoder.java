/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class TekProtocolDecoder extends BaseProtocolDecoder {

    public TekProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number(",d+,")
            .number("(dd)(dd)(dd).d,")           // time (hhmmss)
            .number("(dd)(dd.d+)")               // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+)")              // longitude
            .expression("([EW]),")
            .number("(d+.d+),")                  // hdop
            .number("(d+.d+),")                  // altitude
            .number("(d+),")                     // fix mode
            .number("(d+.d+),")                  // course
            .number("d+.d+,")                    // speed km
            .number("(d+.d+),")                  // speed kn
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(d+),")                     // satellites
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // product type
        buf.readUnsignedByte(); // hardware version
        buf.readUnsignedByte(); // firmware version
        buf.readUnsignedByte(); // contact reason
        buf.readUnsignedByte(); // alarm / status
        buf.readUnsignedByte(); // rssi
        buf.readUnsignedByte(); // battery / status

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        int type = BitUtil.to(buf.readUnsignedByte(), 6);
        buf.readUnsignedByte(); // length

        if (type == 4 || type == 8) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            int count = buf.readUnsignedShort();
            buf.readUnsignedByte(); // hours / tickets
            buf.readUnsignedByte(); // error code
            buf.readUnsignedByte(); // reserved
            buf.readUnsignedByte(); // logger speed
            buf.readUnsignedByte(); // login time
            buf.readUnsignedByte(); // minutes

            for (int i = 0; i < count; i++) {
                position.set("rssi" + (i + 1), buf.readUnsignedByte());
                position.set("temp" + (i + 1), buf.readUnsignedByte() - 30);
                int data = buf.readUnsignedShort();
                position.set("src" + (i + 1), BitUtil.from(data, 10));
                position.set("ullage" + (i + 1), BitUtil.to(data, 10));
            }

            return position;

        } else if (type == 17) {

            String sentence = buf.toString(StandardCharsets.US_ASCII);

            Parser parser = new Parser(PATTERN, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());

            position.set(Position.KEY_HDOP, parser.nextDouble());

            position.setAltitude(parser.nextDouble());
            position.setValid(parser.nextInt() > 0);
            position.setCourse(parser.nextDouble());
            position.setSpeed(parser.nextDouble());

            dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            return position;

        }

        return null;
    }

}
