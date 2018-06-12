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
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class FreematicsProtocolDecoder extends BaseProtocolDecoder {

    public FreematicsProtocolDecoder(FreematicsProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        int startIndex = sentence.indexOf('#');
        int endIndex = sentence.indexOf('*');

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, sentence.substring(0, startIndex));
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setValid(true);

        DateBuilder dateBuilder = new DateBuilder(new Date());

        for (String pair : sentence.substring(startIndex + 1, endIndex).split(",")) {
            String[] data = pair.split("=");
            int key = Integer.parseInt(data[0], 16);
            String value = data[1];
            switch (key) {
                case 0x11:
                    value = ("000000" + value).substring(value.length());
                    dateBuilder.setDateReverse(
                            Integer.parseInt(value.substring(0, 2)),
                            Integer.parseInt(value.substring(2, 4)),
                            Integer.parseInt(value.substring(4)));
                    break;
                case 0x10:
                    value = ("00000000" + value).substring(value.length());
                    dateBuilder.setTime(
                            Integer.parseInt(value.substring(0, 2)),
                            Integer.parseInt(value.substring(2, 4)),
                            Integer.parseInt(value.substring(4, 6)),
                            Integer.parseInt(value.substring(6)) * 10);
                    break;
                case 0xA:
                    position.setLatitude(Double.parseDouble(value));
                    break;
                case 0xB:
                    position.setLongitude(Double.parseDouble(value));
                    break;
                case 0xC:
                    position.setAltitude(Integer.parseInt(value));
                    break;
                case 0xD:
                    position.setLatitude(UnitsConverter.knotsFromKph(Integer.parseInt(value)));
                    break;
                case 0xE:
                    position.setCourse(Integer.parseInt(value));
                    break;
                case 0xF:
                    position.set(Position.KEY_SATELLITES, Integer.parseInt(value));
                    break;
                default:
                    position.set(data[0], value);
                    break;
            }

        }

        position.setTime(dateBuilder.getDate());

        return position;
    }

}
