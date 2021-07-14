/*
 * Copyright 2018 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class WristbandProtocolDecoder extends BaseProtocolDecoder {

    public WristbandProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(
            Channel channel, String imei, String version, int type, String data) {

        if (channel != null) {
            String sentence = String.format("YX%s|%s|0|{F%02d#%s}\r\n", imei, version, type, data);
            ByteBuf response = Unpooled.buffer();
            response.writeBytes(new byte[]{0x00, 0x01, 0x02});
            response.writeShort(sentence.length());
            response.writeCharSequence(sentence, StandardCharsets.US_ASCII);
            response.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xFE, (byte) 0xFC});
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression("..")                    // header
            .number("(d+)|")                     // imei
            .number("([vV]d+.d+)|")              // version
            .number("d+|")                       // model
            .text("{")
            .number("F(d+)")                     // function
            .groupBegin()
            .text("#")
            .expression("(.*)")                  // data
            .groupEnd("?")
            .text("}")
            .text("\r\n")
            .compile();

    private Position decodePosition(DeviceSession deviceSession, String sentence) throws ParseException {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String[] values = sentence.split(",");

        position.setValid(true);
        position.setLongitude(Double.parseDouble(values[0]));
        position.setLatitude(Double.parseDouble(values[1]));
        position.setTime(new SimpleDateFormat("yyyyMMddHHmm").parse(values[2]));
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[3])));

        return position;
    }

    private Position decodeStatus(DeviceSession deviceSession, String sentence) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_BATTERY_LEVEL, Integer.parseInt(sentence.split(",")[0]));

        return position;
    }

    private Position decodeNetwork(DeviceSession deviceSession, String sentence, boolean wifi) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        Network network = new Network();
        String[] fragments = sentence.split("\\|");

        if (wifi) {
            for (String item : fragments[0].split("_")) {
                String[] values = item.split(",");
                network.addWifiAccessPoint(WifiAccessPoint.from(values[0], Integer.parseInt(values[1])));
            }
        }

        for (String item : fragments[wifi ? 1 : 0].split(":")) {
            String[] values = item.split(",");
            int lac = Integer.parseInt(values[0]);
            int mnc = Integer.parseInt(values[1]);
            int mcc = Integer.parseInt(values[2]);
            int cid = Integer.parseInt(values[3]);
            int rssi = Integer.parseInt(values[4]);
            network.addCellTower(CellTower.from(mcc, mnc, lac, cid, rssi));
        }

        position.setNetwork(network);

        return position;
    }

    private List<Position> decodeMessage(
            Channel channel, SocketAddress remoteAddress, String sentence) throws ParseException {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        String version = parser.next();
        int type = parser.nextInt();

        List<Position> positions = new LinkedList<>();
        String data = parser.next();

        switch (type) {
            case 90:
                sendResponse(channel, imei, version, type, getServer(channel, ','));
                break;
            case 91:
                String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
                sendResponse(channel, imei, version, type, time + "|" + getServer(channel, ','));
                break;
            case 1:
                positions.add(decodeStatus(deviceSession, data));
                sendResponse(channel, imei, version, type, data.split(",")[1]);
                break;
            case 2:
                for (String fragment : data.split("\\|")) {
                    positions.add(decodePosition(deviceSession, fragment));
                }
                break;
            case 3:
            case 4:
                positions.add(decodeNetwork(deviceSession, data, type == 3));
                break;
            case 64:
                sendResponse(channel, imei, version, type, data);
                break;
            default:
                break;
        }

        return positions.isEmpty() ? null : positions;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        buf.skipBytes(3); // header
        buf.readUnsignedShort(); // length

        String sentence = buf.toString(buf.readerIndex(), buf.readableBytes() - 3, StandardCharsets.US_ASCII);

        buf.skipBytes(3); // footer

        return decodeMessage(channel, remoteAddress, sentence);
    }

}
