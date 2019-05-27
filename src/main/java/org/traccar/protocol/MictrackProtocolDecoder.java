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
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MictrackProtocolDecoder extends BaseProtocolDecoder {

    public MictrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Date decodeTime(String data) throws ParseException {
        return new SimpleDateFormat("yyMMddHHmmss").parse(data);
    }

    private void decodeLocation(Position position, String data) throws ParseException {
        int index = 0;
        String[] values = data.split("\\+");

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        position.setValid(true);
        position.setTime(decodeTime(values[index++]));
        position.setLatitude(Double.parseDouble(values[index++]));
        position.setLongitude(Double.parseDouble(values[index++]));
        position.setSpeed(UnitsConverter.knotsFromMps(Double.parseDouble(values[index++])));
        position.setCourse(Integer.parseInt(values[index++]));

        position.set(Position.KEY_EVENT, Integer.parseInt(values[index++]));
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) * 0.001);
    }

    private void decodeCell(Network network, String data) {
        String[] values = data.split(",");
        int length = values.length % 5 == 0 ? 5 : 4;
        for (int i = 0; i < values.length / length; i++) {
            int mnc = Integer.parseInt(values[i * length]);
            int lac = Integer.parseInt(values[i * length + 1]);
            int cid = Integer.parseInt(values[i * length + 2]);
            int mcc = Integer.parseInt(values[i * length + 3]);
            network.addCellTower(CellTower.from(mcc, mnc, lac, cid));
        }
    }

    private void decodeWifi(Network network, String data) {
        String[] values = data.split(",");
        for (int i = 0; i < values.length / 2; i++) {
            network.addWifiAccessPoint(WifiAccessPoint.from(values[i * 2], Integer.parseInt(values[i * 2 + 1])));
        }
    }

    private void decodeNetwork(Position position, String data, boolean hasWifi, boolean hasCell) throws ParseException {
        int index = 0;
        String[] values = data.split("\\+");

        getLastLocation(position, decodeTime(values[index++]));

        Network network = new Network();

        if (hasWifi) {
            decodeWifi(network, values[index++]);
        }

        if (hasCell) {
            decodeCell(network, values[index++]);
        }

        position.setNetwork(network);

        position.set(Position.KEY_EVENT, Integer.parseInt(values[index++]));
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) * 0.001);
    }

    private void decodeStatus(Position position, String data) throws ParseException {
        int index = 0;
        String[] values = data.split("\\+");

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        getLastLocation(position, decodeTime(values[index++]));

        index += 4; // fix values

        position.set(Position.KEY_EVENT, Integer.parseInt(values[index++]));
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) * 0.001);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String[] fragments = ((String) msg).split(";");

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, fragments[2]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        switch (fragments[3]) {
            case "R0":
                decodeLocation(position, fragments[4]);
                break;
            case "R1":
                decodeNetwork(position, fragments[4], true, false);
                break;
            case "R2":
            case "R3":
                decodeNetwork(position, fragments[4], false, true);
                break;
            case "R12":
            case "R13":
                decodeNetwork(position, fragments[4], true, true);
                break;
            case "RH":
                decodeStatus(position, fragments[4]);
                break;
            default:
                return null;
        }

        return position;
    }

}
