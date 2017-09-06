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
import org.traccar.model.WifiAccessPoint;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.helper.Log;
import org.traccar.Context;

import java.net.SocketAddress;
import java.util.Date;

import java.nio.charset.StandardCharsets;

public class EelinkProtocolDecoder extends BaseProtocolDecoder {

    public EelinkProtocolDecoder(EelinkProtocol protocol) {
        super(protocol);
    }

    public static final short HEADER_KEY = 0x6767;

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_GPS = 0x02;
    public static final int MSG_HEARTBEAT = 0x03;
    public static final int MSG_ALARM = 0x04;
    public static final int MSG_STATE = 0x05;
    public static final int MSG_SMS = 0x06;
    public static final int MSG_OBD = 0x07;
    public static final int MSG_OBD_FAULT = 0x09;
    public static final int MSG_DOWNLINK = 0x80;
    public static final int MSG_DATA = 0x81;

    // New protocol functions
    public static final int MSG_NORMAL = 0x12;
    public static final int MSG_WARNING = 0x14;
    public static final int MSG_REPORT = 0x15;
    public static final int MSG_COMMAND = 0x16;
    public static final int MSG_OBD_DATA = 0x17;
    public static final int MSG_OBD_BODY = 0x18;
    public static final int MSG_OBD_CODE = 0x19;

    public static final int MSG_CAMERA_INFO = 0x0E;
    public static final int MSG_CAMERA_DATA = 0x0F;

    public static final int MSG_SIGN_COMMAND = 0x01;
    public static final int MSG_SIGN_NEWS = 0x02;

    private void sendResponse(Channel channel, int type, int index, ChannelBuffer content) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.dynamicBuffer();
            response.writeShort(HEADER_KEY);
            response.writeByte(type);
            response.writeShort(2); // initial packet length
            response.writeShort(index);

            if (content != null && content.readableBytes() > 0) {
                response.writeBytes(content);
                response.setShort(3, 2 + content.writerIndex()); // change packet length including buf
            }

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

    private void decodeStatus(Position position, int status) {
        
        position.setValid(BitUtil.check(status, 0));
        
        if (BitUtil.check(status, 1)) {
            position.set(Position.KEY_IGNITION, BitUtil.check(status, 2));
        }
        
        if (BitUtil.check(status, 3)) {
            position.set(Position.KEY_ARMED, BitUtil.check(status, 4));
        }
        
        if (BitUtil.check(status, 5)) {
            position.set(Position.KEY_BLOCKED, !BitUtil.check(status, 6));
        }
        
        if (BitUtil.check(status, 7)) {
            position.set(Position.KEY_CHARGE, BitUtil.check(status, 8));
        }

        // Bit 9 - Device 1-Active/0-Stationary
        // Bit 10 - GPS 1-Running/0-NotRunning
        // Bit 11 - OBD 1-Running/0-NotRunning

        // Digital input 1 - def 0 low
        position.set(Position.PREFIX_IN + 1, BitUtil.check(status, 12));

        // Digital input 2 - def 1 high
        position.set(Position.PREFIX_IN + 2, BitUtil.check(status, 13));

        // Digital input 3 - def 0 low
        position.set(Position.PREFIX_IN + 3, BitUtil.check(status, 14));

        // Digital input 4 - def 1 high
        position.set(Position.PREFIX_IN + 4, BitUtil.check(status, 15));
        
        position.set(Position.KEY_STATUS, status);
    }

    private Position decodeDownlink(ChannelBuffer buf, Position position) {

        getLastLocation(position, new Date());
        position.setValid(false);

        switch (buf.readUnsignedByte()) {
            case MSG_SIGN_COMMAND:
                buf.skipBytes(4); // server flag ignored

                String content = buf.readBytes(buf.readableBytes()).toString(StandardCharsets.UTF_8);

                position.set(Position.KEY_RESULT, content);
                break;
            default:
                return null;
        }

        return position;
    }

    private Position decodeOld(ChannelBuffer buf, Channel channel, Position position, int type, int index) {

        position.setTime(new Date(buf.readUnsignedInt() * 1000));
        position.setLatitude(buf.readInt() / 1800000.0);
        position.setLongitude(buf.readInt() / 1800000.0);
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedShort());

        position.setNetwork(new Network(CellTower.from(
                buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedMedium())));

        position.setValid((buf.readUnsignedByte() & 0x01) != 0);

        switch (type) {
            case MSG_ALARM:
                position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));
                sendResponse(channel, type, index, null);
                break;
            case MSG_STATE:
                switch (buf.readUnsignedByte()) {
                    case 0x01: // ACC on
                    case 0x02: // ACC off
                    case 0x03: // DINx changed
                        buf.skipBytes(4);
                        decodeStatus(position, buf.readUnsignedShort());
                        break;
                    default:
                        break;
                }

                sendResponse(channel, type, index, null);

                break;
            case MSG_GPS:
                // only read remaining 10 bytes if they exist as they are optional in spec
                if (buf.readableBytes() >= 2 * 5) {
                    decodeStatus(position, buf.readUnsignedShort());

                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);

                    position.set(Position.KEY_RSSI, buf.readUnsignedShort());

                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
                }
                break;
            case MSG_SMS:
                String phone = buf.readBytes(21).toString(StandardCharsets.US_ASCII);
                String sanitizedPhone = phone.substring(phone.indexOf("+")).trim();
                int remainingbytes = buf.readableBytes();
                String message = buf.readBytes(remainingbytes).toString(StandardCharsets.UTF_8);
                position.set("phone_no", sanitizedPhone);
                position.set(Position.KEY_RESULT, message);

                String responseMessage;

                switch (message.trim().toUpperCase()) {
                    case "PING?":
                    case "PING#":
                        responseMessage = "PONG";
                        break;
                    default:
                        responseMessage = null;
                        break;
                }

                if (responseMessage != null && !responseMessage.isEmpty()) {
                    ChannelBuffer paddedPhone = ChannelBuffers.buffer(21);
                    paddedPhone.writeBytes(phone.getBytes(StandardCharsets.US_ASCII));
                    paddedPhone.writerIndex(paddedPhone.capacity());

                    ChannelBuffer response = ChannelBuffers.dynamicBuffer();
                    response.writeBytes(paddedPhone);
                    response.writeBytes(responseMessage.getBytes(StandardCharsets.UTF_8));
                    sendResponse(channel, type, index, response);
                } else {
                    sendResponse(channel, type, index, null);
                }

                break;
            default:
                sendResponse(channel, type, index, null);
                break;
        }

        return position;
    }

    private Position decodeNew(ChannelBuffer buf, Channel channel, Position position, int type, int index) {

        Date deviceTime = new Date(buf.readUnsignedInt() * 1000);
        int flags = buf.readUnsignedByte();

        if (BitUtil.check(flags, 0)) {
            position.setLatitude(buf.readInt() / 1800000.0);
            position.setLongitude(buf.readInt() / 1800000.0);
            position.setAltitude(buf.readShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.setValid(Boolean.TRUE);
            position.setTime(deviceTime);
        } else {
            getLastLocation(position, deviceTime);
            position.setValid(Boolean.FALSE);
        }

        Network network = new Network();
        position.setNetwork(network);

        CellTower primaryCellTower = null;

        if (BitUtil.check(flags, 1)) {
            primaryCellTower = CellTower.from(
                    buf.readUnsignedShort(), buf.readUnsignedShort(),
                    buf.readUnsignedShort(), buf.readUnsignedInt(), buf.readUnsignedByte());
            network.addCellTower(primaryCellTower);

            position.set(Position.KEY_RSSI, primaryCellTower.getSignalStrength());
        }

        if (BitUtil.check(flags, 2)) {  // bsid1
            if (BitUtil.check(flags, 1)) {
                network.addCellTower(CellTower.from(
                    primaryCellTower.getMobileCountryCode(), primaryCellTower.getMobileNetworkCode(),
                    buf.readUnsignedShort(), buf.readUnsignedInt(), buf.readUnsignedByte()));
            } else {
                buf.skipBytes(7);
            }
        }

        if (BitUtil.check(flags, 3)) {  // bsid2
            if (BitUtil.check(flags, 1)) {
                network.addCellTower(CellTower.from(
                    primaryCellTower.getMobileCountryCode(), primaryCellTower.getMobileNetworkCode(),
                    buf.readUnsignedShort(), buf.readUnsignedInt(), buf.readUnsignedByte()));
            } else {
                buf.skipBytes(7);
            }
        }

        if (BitUtil.check(flags, 4)) { // bss0
            network.addWifiAccessPoint(
                    WifiAccessPoint.from(ChannelBuffers.hexDump(buf.readBytes(6)), buf.getUnsignedByte(1)));
        }

        if (BitUtil.check(flags, 5)) { // bss1
            network.addWifiAccessPoint(
                    WifiAccessPoint.from(ChannelBuffers.hexDump(buf.readBytes(6)), buf.getUnsignedByte(1)));
        }

        if (BitUtil.check(flags, 6)) { // bss1
            network.addWifiAccessPoint(
                    WifiAccessPoint.from(ChannelBuffers.hexDump(buf.readBytes(6)), buf.getUnsignedByte(1)));
        }

        switch (type) {
            case MSG_WARNING:
                position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));
                sendResponse(channel, type, index, null);
                break;
            case MSG_REPORT:
                switch (buf.readUnsignedByte()) { // status type
                    case 0x01: // ACC on
                    case 0x02: // ACC off
                    case 0x03: // DINx changed
                        decodeStatus(position, buf.readUnsignedShort());
                        break;
                    default:
                        break;
                }
                sendResponse(channel, type, index, null);
                break;
            default:
                break;
        }

        if (buf.readableBytes() >= 2 * 4) {
            decodeStatus(position, buf.readUnsignedShort());

            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);

            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        // check that incoming protocall has correct eelink prefix
        if (buf.readShort() != HEADER_KEY) {
            Log.warning(new IllegalArgumentException("Invalid Header"));
            return null;
        }

        int type = buf.readUnsignedByte();

        short length = buf.readShort();

        if (buf.readableBytes() != length) { // check remaining length matches defined packet length
            Log.warning(new IllegalArgumentException("Packet length error"));
            return null;
        }

        int index = buf.readUnsignedShort();

        if (type == MSG_LOGIN) {
            DeviceSession deviceSession
                    = getDeviceSession(channel, remoteAddress, ChannelBuffers.hexDump(buf.readBytes(8)).substring(1));

            if (deviceSession != null) {
                ChannelBuffer response = ChannelBuffers.dynamicBuffer();
                response.writeLong(new Date().getTime() / 1000);
                response.writeByte(1);
                sendResponse(channel, type, index, response);
            }

            return null;
        } else {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position();
            position.setDeviceId(deviceSession.getDeviceId());
            position.setProtocol(getProtocolName());
            position.set(Position.KEY_INDEX, index);

            if (type == MSG_GPS || type == MSG_ALARM || type == MSG_STATE || type == MSG_SMS) {
                return decodeOld(buf, channel, position, type, index);
            } else if (type == MSG_DOWNLINK) {
                return decodeDownlink(buf, position);
            } else if (type >= MSG_NORMAL && type <= MSG_OBD_CODE) {
                return decodeNew(buf, channel, position, type, index);
            } else if (type == MSG_HEARTBEAT && buf.readableBytes() >= 2) {
                Position last = Context.getIdentityManager().getLastPosition(position.getDeviceId());

                decodeStatus(position, buf.readUnsignedShort());

                if (last != null) {
                    if (last.getInteger(Position.KEY_STATUS) != position.getInteger(Position.KEY_STATUS)) {
                        getLastLocation(position, null);
                        sendResponse(channel, type, index, null);
                        return position;
                    } else {
                        sendResponse(channel, type, index, null);
                        return null;
                    }
                } else {
                    getLastLocation(position, null);
                    sendResponse(channel, type, index, null);
                    return position;
                }
            } else {
                Log.warning(new UnsupportedOperationException());
                sendResponse(channel, type, index, null);
            }
        }

        return null;
    }

}
