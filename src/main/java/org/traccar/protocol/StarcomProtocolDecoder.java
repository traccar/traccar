/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.text.SimpleDateFormat;

public class StarcomProtocolDecoder extends BaseProtocolDecoder {

    public StarcomProtocolDecoder(StarcomProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        sentence = sentence.substring(sentence.indexOf('|') + 1, sentence.lastIndexOf('|'));

        Position position = new Position();
        position.setProtocol(getProtocolName());

        for (String entry : sentence.split(",")) {
            int delimiter = entry.indexOf('=');
            String key = entry.substring(0, delimiter);
            String value = entry.substring(delimiter + 1);
            switch (key) {
                case "unit":
                    DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, value);
                    if (deviceSession != null) {
                        position.setDeviceId(deviceSession.getDeviceId());
                    }
                    break;
                case "gps_valid":
                    position.setValid(Integer.parseInt(value) != 0);
                    break;
                case "datetime_actual":
                    position.setTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(value));
                    break;
                case "latitude":
                    position.setLatitude(Double.parseDouble(value));
                    break;
                case "longitude":
                    position.setLongitude(Double.parseDouble(value));
                    break;
                case "altitude":
                    position.setAltitude(Double.parseDouble(value));
                    break;
                case "velocity":
                    position.setSpeed(UnitsConverter.knotsFromKph(Integer.parseInt(value)));
                    break;
                case "heading":
                    position.setCourse(Integer.parseInt(value));
                    break;
                case "eventid":
                    position.set(Position.KEY_EVENT, Integer.parseInt(value));
                    break;
                case "mileage":
                    position.set(Position.KEY_ODOMETER, (long) (Double.parseDouble(value) * 1000));
                    break;
                case "satellites":
                    position.set(Position.KEY_SATELLITES, Integer.parseInt(value));
                    break;
                case "ignition":
                    position.set(Position.KEY_IGNITION, Integer.parseInt(value) != 0);
                    break;
                case "door":
                    position.set(Position.KEY_DOOR, Integer.parseInt(value) != 0);
                    break;
                case "arm":
                    position.set(Position.KEY_ARMED, Integer.parseInt(value) != 0);
                    break;
                case "fuel":
                    position.set(Position.KEY_FUEL_LEVEL, Integer.parseInt(value));
                    break;
                case "rpm":
                    position.set(Position.KEY_RPM, Integer.parseInt(value));
                    break;
                case "main_voltage":
                    position.set(Position.KEY_POWER, Double.parseDouble(value));
                    break;
                case "backup_voltage":
                    position.set(Position.KEY_BATTERY, Double.parseDouble(value));
                    break;
                case "analog1":
                case "analog2":
                case "analog3":
                    position.set(Position.PREFIX_ADC + (key.charAt(key.length() - 1) - '0'), Double.parseDouble(value));
                    break;
                case "extra1":
                case "extra2":
                case "extra3":
                    position.set(key, value);
                default:
                    break;
            }
        }

        return position;
    }

}
