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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TelemaxProtocolDecoder extends BaseProtocolDecoder {

    public TelemaxProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private String readValue(String sentence, int[] index, int length) {
        String value = sentence.substring(index[0], index[0] + length);
        index[0] += length;
        return value;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("%")) {
            int length = Integer.parseInt(sentence.substring(1, 3));
            getDeviceSession(channel, remoteAddress, sentence.substring(3, 3 + length));
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        int[] index = {0};

        if (!readValue(sentence, index, 1).equals("Y")) {
            return null;
        }

        readValue(sentence, index, 8); // command id
        readValue(sentence, index, 6); // password
        readValue(sentence, index, Integer.parseInt(readValue(sentence, index, 2), 16)); // unit id
        readValue(sentence, index, 2); // frame count

        readValue(sentence, index, 2); // data format

        int interval = Integer.parseInt(readValue(sentence, index, 4), 16);

        readValue(sentence, index, 2); // info flags
        readValue(sentence, index, 2); // version

        int count = Integer.parseInt(readValue(sentence, index, 2), 16);

        Date time = null;
        List<Position> positions = new LinkedList<>();

        for (int i = 0; i < count; i++) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            int speed = Integer.parseInt(readValue(sentence, index, 2), 16);

            position.setValid(BitUtil.check(speed, 7));
            position.setSpeed(BitUtil.to(speed, 7));

            position.setLongitude((Integer.parseInt(readValue(sentence, index, 6), 16) - 5400000) / 30000.0);
            position.setLatitude((Integer.parseInt(readValue(sentence, index, 6), 16) - 5400000) / 30000.0);

            if (i == 0 | i == count - 1) {
                time = new SimpleDateFormat("yyMMddHHmmss").parse(readValue(sentence, index, 12));
                position.set(Position.KEY_STATUS, readValue(sentence, index, 8));
            } else {
                time = new Date(time.getTime() + interval * 1000);
            }

            position.setTime(time);

            positions.add(position);

        }

        return positions;
    }

}
