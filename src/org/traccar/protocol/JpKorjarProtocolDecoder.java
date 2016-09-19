/*
 * Copyright 2016 nyashh (nyashh@gmail.com)
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

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class JpKorjarProtocolDecoder extends BaseProtocolDecoder {

    public JpKorjarProtocolDecoder(JpKorjarProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String line = (String) msg;

        String[] parts = line.split(",");

        if (parts.length == 0) {
            return null;
        }

        if (!parts[0].equals("KORJAR.PL")) {
            return null;
        }

        int year    = Integer.parseInt(parts[2].substring(0, 2));
        int month   = Integer.parseInt(parts[2].substring(2, 4));
        int day     = Integer.parseInt(parts[2].substring(4, 6));
        int hour    = Integer.parseInt(parts[2].substring(6, 8));
        int minute  = Integer.parseInt(parts[2].substring(8, 10));
        int second  = Integer.parseInt(parts[2].substring(10, 12));

        double latitude  = Double.parseDouble(parts[3].substring(0,
                Math.max(0, parts[3].length() - 1)));

        double longitude = Double.parseDouble(parts[4].substring(0,
                Math.max(0, parts[4].length() - 1)));

        double speed     = Double.parseDouble(parts[5]);
        double course    = Double.parseDouble(parts[6]);

        String[] batteryParts = parts[7].split(":");

        double batteryVoltage = Double.parseDouble(batteryParts[1].substring(0,
                Math.max(0, batteryParts[1].length() - 1)));

        String[] codeParts    = parts[8].split(" ");

        int gpsSignal         = Integer.parseInt(codeParts[0]); //0 - low, 1 - high
        int mcc               = Integer.parseInt(codeParts[1]);
        int mnc               = Integer.parseInt(codeParts[2]);
        int lac               = Integer.parseInt(codeParts[3], 16);
        int cid               = Integer.parseInt(codeParts[4], 16);

        DateBuilder builder = new DateBuilder().setDate(year, month, day)
                                               .setTime(hour, minute, second);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parts[1]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setSpeed(speed);
        position.setCourse(course);
        position.set("signal", gpsSignal);
        position.set(Position.KEY_POWER, batteryVoltage);
        position.set(Position.KEY_MNC, mnc);
        position.set(Position.KEY_MCC, mcc);
        position.set(Position.KEY_LAC, lac);
        position.set(Position.KEY_CID, cid);
        position.setTime(builder.getDate());
        position.setValid(true);

        return position;
    }
}
