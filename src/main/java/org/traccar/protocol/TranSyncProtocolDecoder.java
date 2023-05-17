/*
 * Copyright 2013 - 2023 Anton Tananaev (anton@traccar.org)
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
import java.util.Arrays;

public class TranSyncProtocolDecoder extends BaseProtocolDecoder {

    private static final byte[] STX = new byte[]{0x3a, 0x3a};

    private String getHardwareType(int type){

        switch (type) {
            case 1:
                return  "basic";
            case 2:
                return "asset";
            case 3:
                return "bike";
            case 4:
                return "serial";
            case 5:
                return "obd";
            case 6:
                return "l1";
            case 7:
                return "ais-140";
            default:
                return "unknown";
        }
    }

    public TranSyncProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    protected void decodeAlarm(Position position, int value) {
        switch (value) {
            case 10:
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                break;
            case 11:
                position.set(Position.KEY_EVENT, 11);
                break;
            case 16:
                position.set(Position.KEY_EVENT, 16);
                break;
            case 3:
                position.set(Position.KEY_ALARM, 3);
                break;
            case 22:
                position.set(Position.KEY_ALARM, 22);
                break;
            case 9:
                position.set(Position.KEY_EVENT, 9);
            case 17:
                position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
                break;
            case 13:
                position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                break;
            case 14:
                position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                break;
            case 15:
                position.set(Position.KEY_EVENT, 15);
                break;
            case 23:
                position.set(Position.KEY_ALARM, Position.ALARM_ACCIDENT);
                break;
            case 12:
                position.set(Position.KEY_EVENT, 12);
                break;
            case 6:
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_RESTORED);
                break;
            case 4:
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            case 5:
                position.set(Position.KEY_EVENT, 5);
                break;
        }
    }

    private void decodePowerEngineParameters(Position position, int value){

        position.set(Position.PREFIX_OUT + 1, BitUtil.check(value, 7));
        position.set(Position.PREFIX_OUT + 2, BitUtil.check(value, 6));
        position.set(Position.PREFIX_IN + 3, BitUtil.check(value, 5));
        if (BitUtil.check(value, 4)) position.set(Position.KEY_ALARM, Position.ALARM_POWER_OFF);
        position.set(Position.KEY_IGNITION, BitUtil.check(value, 3));
        position.set("gpsFix", BitUtil.check(value, 0));

    }

    private void decodeTrackerStatusParameters(Position position, int value){
        if (BitUtil.check(value, 7)) position.set(Position.KEY_ARCHIVE, true);
        if (BitUtil.check(value, 5)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GPS_ANTENNA_CUT);
        }
        position.set(Position.KEY_VERSION_HW, getHardwareType(BitUtil.between(value, 0, 3)));
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        if (Arrays.equals(ByteBufUtil.getBytes(buf, 0, 2), STX)) {
            buf.readUnsignedShort();
        }
        buf.readByte(); //packetLength

        int locationAreaCode = buf.readUnsignedShort();
        String deviceId = ByteBufUtil.hexDump(buf.readSlice(8));

        buf.readUnsignedShort(); //informationSerialNumber
        buf.readUnsignedByte(); //protocolNumber

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            return null;
        }
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setValid(true);
        position.setTime(new DateBuilder()
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .getDate());
        position.setLatitude(buf.readUnsignedInt() / 1800000.0);
        position.setLongitude(buf.readUnsignedInt() / 1800000.0);
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedShort());

        int mobileNetworkCode = buf.readUnsignedByte();
        int cellTowerId = buf.readUnsignedShort();

        decodePowerEngineParameters(position, buf.readUnsignedByte());

        buf.readUnsignedByte(); // reserved

        decodeAlarm(position, buf.readUnsignedByte());

        decodeTrackerStatusParameters(position, buf.readUnsignedByte());

        int gsmSignalStrength = buf.readUnsignedByte();

        position.set(Position.KEY_BATTERY, (double) (buf.readUnsignedByte() / 10));
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_HDOP, buf.readUnsignedByte());
        position.set(Position.PREFIX_ADC + 1, (short) buf.readUnsignedShort());

        CellTower cellTower = CellTower.fromLacCid(getConfig(), locationAreaCode, cellTowerId);
        cellTower.setMobileNetworkCode(mobileNetworkCode);
        cellTower.setSignalStrength(gsmSignalStrength);

        position.setNetwork(new Network(cellTower));

        if (buf.readableBytes() > 2) {
            buf.readUnsignedByte(); // odometerIndex
            int odometerLength = buf.readUnsignedByte();
            if (odometerLength > 0) {
                int odometer = buf.readBytes(odometerLength).readInt();
                position.set(Position.KEY_ODOMETER, odometer);
            }
            if ((buf.readableBytes() > 2)) {
                buf.readUnsignedByte(); // tagIndex
                int tagLength = buf.readUnsignedByte();
                if (tagLength > 0) {
                    position.set("tag", ByteBufUtil.hexDump(buf.readSlice(tagLength)));
                }
            }
            if ((buf.readableBytes() > 5)) {
                buf.readUnsignedByte(); // adc2Index
                int adc2Length = buf.readUnsignedByte();
                if (adc2Length > 0) {
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
                }
            }
            if ((buf.readableBytes() > 5)) {
                buf.readUnsignedByte(); // adc2Index
                int adc2Length = buf.readUnsignedByte();
                if (adc2Length > 0 && adc2Length <= buf.readableBytes() - 2) {
                    position.set(Position.PREFIX_ADC + 3, buf.readUnsignedShort());
                }
            }
        }
        return position;
    }
}
