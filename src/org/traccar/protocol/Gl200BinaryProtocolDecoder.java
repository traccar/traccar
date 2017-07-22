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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Gl200BinaryProtocolDecoder extends BaseProtocolDecoder {

    public Gl200BinaryProtocolDecoder(Gl200Protocol protocol) {
        super(protocol);
    }

    private Date decodeTime(ChannelBuffer buf) {
        DateBuilder dateBuilder = new DateBuilder()
                .setDate(buf.readUnsignedShort(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        return dateBuilder.getDate();
    }

    private Position decodeEvent(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) {

        Position position = new Position();
        position.setProtocol(getProtocolName());

        buf.readUnsignedByte(); // message type
        buf.readUnsignedInt(); // mask
        buf.readUnsignedShort(); // length
        buf.readUnsignedByte(); // device type
        buf.readUnsignedShort(); // protocol version

        position.set(Position.KEY_VERSION_FW, String.valueOf(buf.readUnsignedShort()));

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.format("%015d", buf.readLong()));
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
        position.set(Position.KEY_POWER, buf.readUnsignedShort());

        buf.readUnsignedByte(); // motion status

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_INDEX, buf.readUnsignedByte());

        int hdop = buf.readUnsignedByte();
        position.setValid(hdop > 0);
        position.set(Position.KEY_HDOP, hdop);

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedMedium()));
        position.setCourse(buf.readUnsignedShort());
        position.setAltitude(buf.readShort());
        position.setLongitude(buf.readInt() * 0.000001);
        position.setLatitude(buf.readInt() * 0.000001);

        position.setTime(decodeTime(buf));

        position.setNetwork(new Network(CellTower.from(
                buf.readUnsignedShort(), buf.readUnsignedShort(),
                buf.readUnsignedShort(), buf.readUnsignedShort())));

        buf.readUnsignedByte(); // reserved

        return position;
    }

    public static final int MSG_INF_GPS = 2;
    public static final int MSG_INF_CID = 4;
    public static final int MSG_INF_CSQ = 5;
    public static final int MSG_INF_VER = 6;
    public static final int MSG_INF_BAT = 7;
    public static final int MSG_INF_TMZ = 9;
    public static final int MSG_INF_GIR = 10;

    private Position decodeInformation(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) {

        Position position = new Position();
        position.setProtocol(getProtocolName());

        int type = buf.readUnsignedByte();

        buf.readUnsignedInt(); // mask
        buf.readUnsignedShort(); // length

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.format("%015d", buf.readLong()));
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedByte(); // device type
        buf.readUnsignedShort(); // protocol version

        position.set(Position.KEY_VERSION_FW, String.valueOf(buf.readUnsignedShort()));

        if (type == MSG_INF_VER) {
            buf.readUnsignedShort(); // hardware version
            buf.readUnsignedShort(); // mcu version
            buf.readUnsignedShort(); // reserved
        }

        buf.readUnsignedByte(); // motion status
        buf.readUnsignedByte(); // reserved

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

        buf.readUnsignedByte(); // mode
        buf.skipBytes(7); // last fix time
        buf.readUnsignedByte(); // reserved
        buf.readUnsignedByte();
        buf.readUnsignedShort(); // response report mask
        buf.readUnsignedShort(); // ign interval
        buf.readUnsignedShort(); // igf interval
        buf.readUnsignedInt(); // reserved
        buf.readUnsignedByte(); // reserved

        if (type == MSG_INF_BAT) {
            position.set(Position.KEY_CHARGE, buf.readUnsignedByte() != 0);
            position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
            position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
        }

        buf.skipBytes(10); // iccid

        if (type == MSG_INF_CSQ) {
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            buf.readUnsignedByte();
        }

        buf.readUnsignedByte(); // time zone flags
        buf.readUnsignedShort(); // time zone offset

        if (type == MSG_INF_GIR) {
            buf.readUnsignedByte(); // gir trigger
            buf.readUnsignedByte(); // cell number
            position.setNetwork(new Network(CellTower.from(
                    buf.readUnsignedShort(), buf.readUnsignedShort(),
                    buf.readUnsignedShort(), buf.readUnsignedShort())));
            buf.readUnsignedByte(); // ta
            buf.readUnsignedByte(); // rx level
        }

        getLastLocation(position, decodeTime(buf));

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        switch (buf.readBytes(4).toString(StandardCharsets.US_ASCII)) {
            case "+INF":
                return decodeInformation(channel, remoteAddress, buf);
            case "+EVT":
                return decodeEvent(channel, remoteAddress, buf);
            default:
                return null;
        }
    }

}
