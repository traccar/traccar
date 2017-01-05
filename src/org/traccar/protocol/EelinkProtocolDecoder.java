/*
 * Copyright 2014 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class EelinkProtocolDecoder extends BaseProtocolDecoder {

    public EelinkProtocolDecoder(EelinkProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_GPS = 0x02;
    public static final int MSG_HEARTBEAT = 0x03;
    public static final int MSG_ALARM = 0x04;
    public static final int MSG_STATE = 0x05;
    public static final int MSG_SMS = 0x06;
    public static final int MSG_OBD = 0x07;
    public static final int MSG_DOWNLINK = 0x80;
    public static final int MSG_DATA = 0x81;

    public static final int MSG_NORMAL = 0x12;
    public static final int MSG_WARNING = 0x14;
    public static final int MSG_REPORT = 0x15;
    public static final int MSG_COMMAND = 0x16;
    public static final int MSG_OBD_DATA = 0x17;
    public static final int MSG_OBD_BODY = 0x18;
    public static final int MSG_OBD_CODE = 0x19;
    public static final int MSG_CAMERA_INFO = 0x1E;
    public static final int MSG_CAMERA_DATA = 0x1F;

    private void sendResponse(Channel channel, int type, int index) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.buffer(7);
            response.writeByte(0x67); response.writeByte(0x67); // header
            response.writeByte(type);
            response.writeShort(2); // length
            response.writeShort(index);
            channel.write(response);
        }
    }

    private String decodeAlarm(Short value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_POWER_OFF;
            case 0x02:
                return Position.ALARM_SOS;
            case 0x03:
                return Position.ALARM_LOW_BATTERY;
            case 0x04:
                return Position.ALARM_VIBRATION;
            case 0x08:
            case 0x09:
                return Position.ALARM_GPS_ANTENNA_CUT;
            case 0x81:
                return Position.ALARM_LOW_SPEED;
            case 0x82:
                return Position.ALARM_OVERSPEED;
            case 0x83:
                return Position.ALARM_GEOFENCE_ENTER;
            case 0x84:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x85:
                return Position.ALARM_ACCIDENT;
            case 0x86:
                return Position.ALARM_FALL_DOWN;
            default:
                return null;
        }
    }

    private Position decodeOld(DeviceSession deviceSession, ChannelBuffer buf, int type, int index) {

        Position position = new Position();
        position.setDeviceId(deviceSession.getDeviceId());
        position.setProtocol(getProtocolName());

        position.set(Position.KEY_INDEX, index);

        position.setTime(new Date(buf.readUnsignedInt() * 1000));
        position.setLatitude(buf.readInt() / 1800000.0);
        position.setLongitude(buf.readInt() / 1800000.0);
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedShort());

        position.setNetwork(new Network(CellTower.from(
                buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedMedium())));

        position.setValid((buf.readUnsignedByte() & 0x01) != 0);

        if (type == MSG_ALARM) {
            position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));
        }

        if (buf.readableBytes() >= 2 * 5) {

            int status = buf.readUnsignedShort();
            if (BitUtil.check(status, 1)) {
                position.set(Position.KEY_IGNITION, BitUtil.check(status, 2));
            }
            if (BitUtil.check(status, 7)) {
                position.set(Position.KEY_CHARGE, BitUtil.check(status, 8));
            }
            position.set(Position.KEY_STATUS, status);

            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() + "mV");

            buf.readUnsignedShort(); // signal strength

            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());

        }

        return position;
    }

    private Position decodeNew(DeviceSession deviceSession, ChannelBuffer buf, int index) {

        Position position = new Position();
        position.setDeviceId(deviceSession.getDeviceId());
        position.setProtocol(getProtocolName());

        position.set(Position.KEY_INDEX, index);

        position.setTime(new Date(buf.readUnsignedInt() * 1000));

        int flags = buf.readUnsignedByte();

        if (BitUtil.check(flags, 0)) {
            position.setLatitude(buf.readInt() / 1800000.0);
            position.setLongitude(buf.readInt() / 1800000.0);
            position.setAltitude(buf.readShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        if (BitUtil.check(flags, 1)) {
            position.setNetwork(new Network(CellTower.from(
                    buf.readUnsignedShort(), buf.readUnsignedShort(),
                    buf.readUnsignedShort(), buf.readUnsignedInt(), buf.readUnsignedByte())));
        }

        if (BitUtil.check(flags, 2)) {
            buf.skipBytes(7); // bsid1
        }

        if (BitUtil.check(flags, 3)) {
            buf.skipBytes(7); // bsid2
        }

        if (BitUtil.check(flags, 4)) {
            buf.skipBytes(7); // bss0
        }

        if (BitUtil.check(flags, 5)) {
            buf.skipBytes(7); // bss1
        }

        if (BitUtil.check(flags, 6)) {
            buf.skipBytes(7); // bss2
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readShort(); // length
        int index = buf.readUnsignedShort();

        if (type != MSG_GPS && type != MSG_DATA) {
            sendResponse(channel, type, index);
        }

        if (type == MSG_LOGIN) {

            getDeviceSession(channel, remoteAddress, ChannelBuffers.hexDump(buf.readBytes(8)).substring(1));

        } else {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            if (type == MSG_GPS || type == MSG_ALARM || type == MSG_STATE || type == MSG_SMS) {
                return decodeOld(deviceSession, buf, type, index);
            } else if (type >= MSG_NORMAL && type <= MSG_OBD_CODE) {
                return decodeNew(deviceSession, buf, index);
            }
        }

        return null;
    }

}
