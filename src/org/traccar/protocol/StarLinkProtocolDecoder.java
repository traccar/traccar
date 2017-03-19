/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class StarLinkProtocolDecoder extends BaseProtocolDecoder {

    private String[] dataTags;
    private DateFormat dateFormat;

    public StarLinkProtocolDecoder(StarLinkProtocol protocol) {
        super(protocol);

        String format = Context.getConfig().getString(
                getProtocolName() + ".format", "#EDT#,#EID#,#PDT#,#LAT#,#LONG#,#SPD#,#HEAD#,#ODO#,"
                + "#IN1#,#IN2#,#IN3#,#IN4#,#OUT1#,#OUT2#,#OUT3#,#OUT4#,#LAC#,#CID#,#VIN#,#VBAT#,#DEST#,#IGN#,#ENG#");
        dataTags = format.split(",");

        dateFormat = new SimpleDateFormat(
                Context.getConfig().getString(getProtocolName() + ".dateFormat", "yyMMddHHmmss"));
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression(".")                     // protocol head
            .text("SLU")                         // message head
            .number("(x{6}|d{15}),")             // id
            .number("(d+),")                     // type
            .number("(d+),")                     // index
            .expression("(.+)")                  // data
            .text("*")
            .number("xx")                        // checksum
            .compile();

    public static final int MSG_EVENT_REPORT = 6;

    private double parseCoordinate(String value) {
        int minutesIndex = value.indexOf('.') - 2;
        double result = Double.parseDouble(value.substring(1, minutesIndex));
        result += Double.parseDouble(value.substring(minutesIndex)) / 60;
        return value.charAt(0) == '+' ? result : -result;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        int type = parser.nextInt();
        if (type != MSG_EVENT_REPORT) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setValid(true);

        position.set(Position.KEY_INDEX, parser.nextInt());

        String[] data = parser.next().split(",");
        Integer lac = null, cid = null;
        int event = 0;

        for (int i = 0; i < Math.min(data.length, dataTags.length); i++) {
            switch (dataTags[i]) {
                case "#EDT#":
                    position.setDeviceTime(dateFormat.parse(data[i]));
                    break;
                case "#EID#":
                    event = Integer.parseInt(data[i]);
                    position.set(Position.KEY_EVENT, event);
                    break;
                case "#PDT#":
                    position.setFixTime(dateFormat.parse(data[i]));
                    break;
                case "#LAT#":
                    position.setLatitude(parseCoordinate(data[i]));
                    break;
                case "#LONG#":
                    position.setLongitude(parseCoordinate(data[i]));
                    break;
                case "#SPD#":
                    position.setSpeed(Double.parseDouble(data[i]));
                    break;
                case "#HEAD#":
                    position.setCourse(Integer.parseInt(data[i]));
                    break;
                case "#ODO#":
                    position.set(Position.KEY_ODOMETER, Long.parseLong(data[i]) * 1000);
                    break;
                case "#IN1#":
                    position.set(Position.PREFIX_IN + 1, Integer.parseInt(data[i]));
                    break;
                case "#IN2#":
                    position.set(Position.PREFIX_IN + 2, Integer.parseInt(data[i]));
                    break;
                case "#IN3#":
                    position.set(Position.PREFIX_IN + 3, Integer.parseInt(data[i]));
                    break;
                case "#IN4#":
                    position.set(Position.PREFIX_IN + 4, Integer.parseInt(data[i]));
                    break;
                case "#OUT1#":
                    position.set(Position.PREFIX_OUT + 1, Integer.parseInt(data[i]));
                    break;
                case "#OUT2#":
                    position.set(Position.PREFIX_OUT + 2, Integer.parseInt(data[i]));
                    break;
                case "#OUT3#":
                    position.set(Position.PREFIX_OUT + 3, Integer.parseInt(data[i]));
                    break;
                case "#OUT4#":
                    position.set(Position.PREFIX_OUT + 4, Integer.parseInt(data[i]));
                    break;
                case "#LAC#":
                    lac = Integer.parseInt(data[i]);
                    break;
                case "#CID#":
                    cid = Integer.parseInt(data[i]);
                    break;
                case "#VIN#":
                    position.set(Position.KEY_POWER, Double.parseDouble(data[i]));
                    break;
                case "#VBAT#":
                    position.set(Position.KEY_BATTERY, Double.parseDouble(data[i]));
                    break;
                case "#DEST#":
                    position.set("destination", data[i]);
                    break;
                case "#IGN#":
                    position.set(Position.KEY_IGNITION, data[i].equals("1"));
                    break;
                case "#ENG#":
                    position.set("engine", data[i].equals("1"));
                    break;
                default:
                    break;
            }
        }

        if (lac != null && cid != null) {
            position.setNetwork(new Network(CellTower.fromLacCid(lac, cid)));
        }

        if (event == 20) {
            position.set(Position.KEY_RFID, data[data.length - 1]);
        }

        return position;
    }

}
