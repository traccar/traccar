/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitBuffer;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Gl200BinaryProtocolDecoder extends BaseProtocolDecoder {

    public Gl200BinaryProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Date decodeTime(ByteBuf buf) {
        DateBuilder dateBuilder = new DateBuilder()
                .setDate(buf.readUnsignedShort(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        return dateBuilder.getDate();
    }

    public static final int MSG_RSP_LCB = 3;
    public static final int MSG_RSP_GEO = 8;
    public static final int MSG_RSP_COMPRESSED = 100;

    private List<Position> decodeLocation(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        List<Position> positions = new LinkedList<>();

        int type = buf.readUnsignedByte();

        buf.readUnsignedInt(); // mask
        buf.readUnsignedShort(); // length
        buf.readUnsignedByte(); // device type
        buf.readUnsignedShort(); // protocol version
        buf.readUnsignedShort(); // firmware version

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.format("%015d", buf.readLong()));
        if (deviceSession == null) {
            return null;
        }

        int battery = buf.readUnsignedByte();
        int power = buf.readUnsignedShort();

        if (type == MSG_RSP_GEO) {
            buf.readUnsignedByte(); // reserved
            buf.readUnsignedByte(); // reserved
        }

        buf.readUnsignedByte(); // motion status
        int satellites = buf.readUnsignedByte();

        if (type != MSG_RSP_COMPRESSED) {
            buf.readUnsignedByte(); // index
        }

        if (type == MSG_RSP_LCB) {
            buf.readUnsignedByte(); // phone length
            for (int b = buf.readUnsignedByte();; b = buf.readUnsignedByte()) {
                if ((b & 0xf) == 0xf || (b & 0xf0) == 0xf0) {
                    break;
                }
            }
        }

        if (type == MSG_RSP_COMPRESSED) {

            int count = buf.readUnsignedShort();

            BitBuffer bits;
            int speed = 0;
            int heading = 0;
            int latitude = 0;
            int longitude = 0;
            long time = 0;

            for (int i = 0; i < count; i++) {

                if (time > 0) {
                    time += 1;
                }

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                switch (BitUtil.from(buf.getUnsignedByte(buf.readerIndex()), 8 - 2)) {
                    case 1:
                        bits = new BitBuffer(buf.readSlice(3));
                        bits.readUnsigned(2); // point attribute
                        bits.readUnsigned(1); // fix type
                        speed = bits.readUnsigned(12);
                        heading = bits.readUnsigned(9);
                        longitude = buf.readInt();
                        latitude = buf.readInt();
                        if (time == 0) {
                            time = buf.readUnsignedInt();
                        }
                        break;
                    case 2:
                        bits = new BitBuffer(buf.readSlice(5));
                        bits.readUnsigned(2); // point attribute
                        bits.readUnsigned(1); // fix type
                        speed += bits.readSigned(7);
                        heading += bits.readSigned(7);
                        longitude += bits.readSigned(12);
                        latitude += bits.readSigned(11);
                        break;
                    default:
                        buf.readUnsignedByte(); // invalid or same
                        continue;
                }

                position.setValid(true);
                position.setTime(new Date(time * 1000));
                position.setSpeed(UnitsConverter.knotsFromKph(speed * 0.1));
                position.setCourse(heading);
                position.setLongitude(longitude * 0.000001);
                position.setLatitude(latitude * 0.000001);

                positions.add(position);

            }

        } else {

            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.set(Position.KEY_BATTERY_LEVEL, battery);
                position.set(Position.KEY_POWER, power);
                position.set(Position.KEY_SATELLITES, satellites);

                int hdop = buf.readUnsignedByte();
                position.setValid(hdop > 0);
                position.set(Position.KEY_HDOP, hdop);

                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedMedium() * 0.1));
                position.setCourse(buf.readUnsignedShort());
                position.setAltitude(buf.readShort());
                position.setLongitude(buf.readInt() * 0.000001);
                position.setLatitude(buf.readInt() * 0.000001);

                position.setTime(decodeTime(buf));

                position.setNetwork(new Network(CellTower.from(
                        buf.readUnsignedShort(), buf.readUnsignedShort(),
                        buf.readUnsignedShort(), buf.readUnsignedShort())));

                buf.readUnsignedByte(); // reserved

                positions.add(position);

            }

        }

        return positions;
    }

    public static final int MSG_EVT_BPL = 6;
    public static final int MSG_EVT_VGN = 45;
    public static final int MSG_EVT_VGF = 46;
    public static final int MSG_EVT_UPD = 15;
    public static final int MSG_EVT_IDF = 17;
    public static final int MSG_EVT_GSS = 21;
    public static final int MSG_EVT_GES = 26;
    public static final int MSG_EVT_GPJ = 31;
    public static final int MSG_EVT_RMD = 35;
    public static final int MSG_EVT_JDS = 33;
    public static final int MSG_EVT_CRA = 23;
    public static final int MSG_EVT_UPC = 34;

    private Position decodeEvent(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        Position position = new Position(getProtocolName());

        int type = buf.readUnsignedByte();

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

        switch (type) {
            case MSG_EVT_BPL -> buf.readUnsignedShort(); // backup battery voltage
            case MSG_EVT_VGN, MSG_EVT_VGF -> {
                buf.readUnsignedShort(); // reserved
                buf.readUnsignedByte(); // report type
                buf.readUnsignedInt(); // ignition duration
            }
            case MSG_EVT_UPD -> {
                buf.readUnsignedShort(); // code
                buf.readUnsignedByte(); // retry
            }
            case MSG_EVT_IDF -> buf.readUnsignedInt(); // idling duration
            case MSG_EVT_GSS -> {
                buf.readUnsignedByte(); // gps signal status
                buf.readUnsignedInt(); // reserved
            }
            case MSG_EVT_GES -> {
                buf.readUnsignedShort(); // trigger geo id
                buf.readUnsignedByte(); // trigger geo enable
                buf.readUnsignedByte(); // trigger mode
                buf.readUnsignedInt(); // radius
                buf.readUnsignedInt(); // check interval
            }
            case MSG_EVT_GPJ -> {
                buf.readUnsignedByte(); // cw jamming value
                buf.readUnsignedByte(); // gps jamming state
            }
            case MSG_EVT_RMD -> buf.readUnsignedByte(); // roaming state
            case MSG_EVT_JDS -> buf.readUnsignedByte(); // jamming state
            case MSG_EVT_CRA -> buf.readUnsignedByte(); // crash counter
            case MSG_EVT_UPC -> {
                buf.readUnsignedByte(); // command id
                buf.readUnsignedShort(); // result
            }
        }

        buf.readUnsignedByte(); // count

        int hdop = buf.readUnsignedByte();
        position.setValid(hdop > 0);
        position.set(Position.KEY_HDOP, hdop);

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedMedium() * 0.1));
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

    private Position decodeInformation(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        Position position = new Position(getProtocolName());

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

        ByteBuf buf = (ByteBuf) msg;

        return switch (buf.readSlice(4).toString(StandardCharsets.US_ASCII)) {
            case "+RSP" -> decodeLocation(channel, remoteAddress, buf);
            case "+INF" -> decodeInformation(channel, remoteAddress, buf);
            case "+EVT" -> decodeEvent(channel, remoteAddress, buf);
            default -> null;
        };
    }

}
