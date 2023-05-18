/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;

public class TranSyncProtocolDecoder extends BaseProtocolDecoder {

    public TranSyncProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private String decodeAlarm(int value) {
        switch (value) {
            case 4:
                return Position.ALARM_LOW_BATTERY;
            case 6:
                return Position.ALARM_POWER_RESTORED;
            case 10:
                return Position.ALARM_SOS;
            case 13:
                return Position.ALARM_BRAKING;
            case 14:
                return Position.ALARM_ACCELERATION;
            case 17:
                return Position.ALARM_OVERSPEED;
            case 23:
                return Position.ALARM_ACCIDENT;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedShort(); // header
        buf.readByte(); // length

        int lac = buf.readUnsignedShort();

        String deviceId = ByteBufUtil.hexDump(buf.readSlice(8));
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedShort(); // index
        buf.readUnsignedByte(); // type

        position.setTime(new DateBuilder()
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .getDate());

        double latitude = buf.readUnsignedInt() / 1800000.0;
        double longitude = buf.readUnsignedInt() / 1800000.0;

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedShort());

        int mnc = buf.readUnsignedByte();
        int cid = buf.readUnsignedShort();
        int status0 = buf.readUnsignedByte();

        position.setValid(BitUtil.check(status0, 0));
        position.setLatitude(BitUtil.check(status0, 1) ? latitude : -latitude);
        position.setLongitude(BitUtil.check(status0, 2) ? longitude : -longitude);

        position.set(Position.PREFIX_OUT + 1, BitUtil.check(status0, 7));
        position.set(Position.PREFIX_OUT + 2, BitUtil.check(status0, 6));
        position.set(Position.PREFIX_IN + 3, BitUtil.check(status0, 5));
        if (BitUtil.check(status0, 4)) {
            position.set(Position.KEY_ALARM, Position.ALARM_POWER_OFF);
        }
        position.set(Position.KEY_IGNITION, BitUtil.check(status0, 3));

        buf.readUnsignedByte(); // reserved

        int event = buf.readUnsignedByte();
        position.set(Position.KEY_ALARM, decodeAlarm(event));
        position.set(Position.KEY_EVENT, event);

        int status3 = buf.readUnsignedByte();
        if (BitUtil.check(status3, 7)) {
            position.set(Position.KEY_ARCHIVE, true);
        }
        if (BitUtil.check(status3, 5)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GPS_ANTENNA_CUT);
        }

        int rssi = buf.readUnsignedByte();
        CellTower cellTower = CellTower.fromLacCid(getConfig(), lac, cid);
        cellTower.setMobileNetworkCode(mnc);
        cellTower.setSignalStrength(rssi);
        position.setNetwork(new Network(cellTower));

        position.set(Position.KEY_BATTERY, (double) (buf.readUnsignedByte() / 10));
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_HDOP, buf.readUnsignedByte());
        position.set(Position.PREFIX_ADC + 1, (short) buf.readUnsignedShort());

        if (buf.readableBytes() > 5) {
            buf.readUnsignedByte(); // odometer id
            int length = buf.readUnsignedByte();
            if (length > 0) {
                position.set(Position.KEY_ODOMETER, buf.readBytes(length).readInt());
            }
        }
        if (buf.readableBytes() > 5) {
            buf.readUnsignedByte(); // rfid id
            int length = buf.readUnsignedByte();
            if (length > 0) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, ByteBufUtil.hexDump(buf.readSlice(length)));
            }
        }
        if (buf.readableBytes() > 5) {
            buf.readUnsignedByte(); // adc2 id
            int length = buf.readUnsignedByte();
            if (length > 0) {
                position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
            }
        }
        if (buf.readableBytes() > 5) {
            buf.readUnsignedByte(); // adc3 id
            int length = buf.readUnsignedByte();
            if (length > 0 && length <= buf.readableBytes() - 2) {
                position.set(Position.PREFIX_ADC + 3, buf.readUnsignedShort());
            }
        }

        return position;
    }

}
