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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;


public class ZrProtocolDecoder extends BaseProtocolDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZrProtocolDecoder.class);

    private static final int MSG_HEARTBEAT = 0x0100;
    private static final int MSG_DEV_INFO = 0x0101;
    private static final int MSG_REPORT = 0x0102;
    private static final int MSG_SER_GEN_ACK = 0x0400;
    private static final int MSG_LBS_TRANSFER_LONG_LAT = 0x0103;
    private static final int MSG_LBS_TRANSFER_LONG_LAT_ACK = 0x0403;
    private static final int MSG_TIME_SYN = 0x0104;
    private static final int MSG_TIME_SYN_ACK = 0x0404;
    public static final int MSG_CFG = 0x0700;
    private static final int MSG_DEV_GEN_ACK = 0x0a00;
    public static final int MSG_QUERY = 0x0701;
    private static final int MSG_QUERY_ACK = 0x0a01;

    public ZrProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        buf.readShort(); // skip header
        buf.readUnsignedShort();
        short editionNum = buf.readShort();
        byte encryptionType = buf.readByte();
        ByteBuf id = buf.readSlice(10);
        int msgType = buf.readUnsignedShort();
        ByteBufUtil.hexDump(buf.readSlice(1));
        Integer packageNo = buf.readUnsignedShort();
        int bodyLen = buf.readUnsignedByte();
        ByteBuf bodyBuf = buf.readSlice(bodyLen);
        buf.readByte();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, decodeId(id));
        if (deviceSession == null) {
            return null;
        }

        if (!deviceSession.contains(DeviceSession.KEY_TIMEZONE)) {
            deviceSession.set(DeviceSession.KEY_TIMEZONE, getTimeZone(deviceSession.getDeviceId(), "UTC"));
        }

        switch (msgType) {
            case MSG_HEARTBEAT:
            case MSG_DEV_INFO:
                sendGeneralResponse(channel, remoteAddress, id, msgType, editionNum, encryptionType, packageNo);
                break;
            case MSG_REPORT:
                sendGeneralResponse(channel, remoteAddress, id, msgType, editionNum, encryptionType, packageNo);
                // GPS data
                return decodeFrameTypeReport(deviceSession, bodyBuf);
            case MSG_LBS_TRANSFER_LONG_LAT:
                sendLbsTransferResponse(channel, remoteAddress, id, msgType, editionNum, encryptionType, packageNo);
                break;
            case MSG_TIME_SYN:
                sendTimeSynResponse(channel, remoteAddress, deviceSession, id, msgType, editionNum, encryptionType, packageNo);
                break;
            case MSG_DEV_GEN_ACK:
            case MSG_QUERY_ACK:
                // nothing to do
                break;
            default:
                LOGGER.error("zr protocol decoder not support this frame type:{}", msgType);
        }
        return null;
    }


    private Position decodeFrameTypeReport(DeviceSession deviceSession, ByteBuf buf) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        while (buf.readableBytes() > 0) {
            int tag = buf.readUnsignedShort();
            int len = buf.readUnsignedShort();
            ByteBuf valueBuf = buf.readSlice(len);

            switch (tag) {
                case 0x2095:
                    position.setAccuracy((double) valueBuf.readUnsignedShort() / 100);
                    BigDecimal speedDecimal = BigDecimal.valueOf(valueBuf.readUnsignedShort() * 0.1 / 1.852);
                    BigDecimal speedRounded = speedDecimal.setScale(2, RoundingMode.HALF_UP);
                    position.setSpeed(speedRounded.doubleValue());
                    position.setCourse(valueBuf.readUnsignedShort());
                    position.setAltitude(valueBuf.readInt());
                    position.setLongitude(valueBuf.readInt() * 0.000001);
                    position.setLatitude(valueBuf.readInt() * 0.000001);
                    long utcSecondTime = valueBuf.readUnsignedInt();
                    Calendar calendar = Calendar.getInstance((TimeZone) deviceSession.get(DeviceSession.KEY_TIMEZONE));
                    calendar.setTimeInMillis(utcSecondTime * 1000L);
                    position.setTime(calendar.getTime());
                    break;
                case 0x240c:
                    position.set(Position.KEY_ODOMETER, valueBuf.readUnsignedInt());
                    break;
                case 0x2310:
                    long value = valueBuf.readLong();
                    position.set(Position.KEY_ALARM, decodeAlarmTag(value));

                    if (BitUtil.check(value, 12)) {
                        deviceSession.set(Position.KEY_IGNITION, true);
                    }
                    if (BitUtil.check(value, 13)) {
                        deviceSession.set(Position.KEY_IGNITION, false);
                    }
                    break;
            }
        }

        if (position.getLatitude() != 0 && position.getLongitude() != 0) {
            position.setValid(true);
        } else {
            return null;
        }

        Optional<Boolean> accStateOptional = Optional.ofNullable(deviceSession.get(Position.KEY_IGNITION));
        position.set(Position.KEY_IGNITION, accStateOptional.orElse(false));
        return position;
    }

    private String decodeAlarmTag(long value) {
        if (BitUtil.check(value, 3)) {
            return Position.ALARM_POWER_OFF;
        }
        if (BitUtil.check(value, 4)) {
            return Position.ALARM_LOW_POWER;
        }
        if (BitUtil.check(value, 22)) {
            return Position.ALARM_OVERSPEED;
        }
        if (BitUtil.check(value, 41)) {
            return Position.ALARM_TOW;
        }
        if (BitUtil.check(value, 42)) {
            return Position.ALARM_FALL_DOWN;
        }
        if (BitUtil.check(value, 43)) {
            return Position.ALARM_ACCIDENT;
        }
        if (BitUtil.check(value, 44)) {
            return Position.ALARM_VIBRATION;
        }
        if (BitUtil.check(value, 50)) {
            return Position.ALARM_REMOVING;
        }

        return null;
    }

    private String decodeId(ByteBuf id) {
        String str = ByteBufUtil.hexDump(id);
        String replaceLeftZeoId = str.replaceAll("^0+", "");
        return replaceLeftZeoId.isEmpty() ? "0" : replaceLeftZeoId;
    }

    public static ByteBuf formatMessage(int type, ByteBuf id, short editionNum, byte encryptionType, Integer packageNo, ByteBuf body) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(0xdddd);
        buffer.writeShort(body.readableBytes() + 20);
        buffer.writeShort(editionNum);
        buffer.writeByte(encryptionType);
        buffer.writeBytes(id);
        buffer.writeShort(type);
        buffer.writeByte(0x11);
        buffer.writeShort(packageNo);
        buffer.writeByte(body.readableBytes());
        buffer.writeBytes(body);
        buffer.writeByte(Checksum.xor(buffer.nioBuffer(2, buffer.writerIndex() - 2))); // check sum
        buffer.writeShort(0xffff);
        return buffer;
    }

    private static void sendGeneralResponse(Channel channel, SocketAddress remoteAddress, ByteBuf id, int frameType, short editionNum, byte encryptionType, Integer packageNo) {
        ByteBuf response = Unpooled.buffer();
        addAuthTag(response);
        addGeneralResponseTag(response, frameType, packageNo);
        addUtcTimeTag(response);
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(formatMessage(MSG_SER_GEN_ACK, id, editionNum, encryptionType, packageNo, response), remoteAddress));
        }
    }

    private static void sendLbsTransferResponse(Channel channel, SocketAddress remoteAddress, ByteBuf id, int frameType, short editionNum, byte encryptionType, Integer packageNo) {
        ByteBuf response = Unpooled.buffer();
        addAuthTag(response);
        addLngLatRespTag(response);
        addUtcTimeTag(response);
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(formatMessage(MSG_LBS_TRANSFER_LONG_LAT_ACK, id, editionNum, encryptionType, packageNo, response), remoteAddress));
        }
    }

    private static void sendTimeSynResponse(Channel channel, SocketAddress remoteAddress, DeviceSession deviceSession, ByteBuf id, int frameType, short editionNum, byte encryptionType, Integer packageNo) {
        ByteBuf response = Unpooled.buffer();
        addAuthTag(response);
        addTimeSynTag(response, deviceSession);
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(formatMessage(MSG_TIME_SYN_ACK, id, editionNum, encryptionType, packageNo, response), remoteAddress));
        }
    }

    public static void addAuthTag(ByteBuf bodyBuf) {
        bodyBuf.writeShort(0x2391);
        bodyBuf.writeShort(3);
        bodyBuf.writeBytes(new byte[]{0x12, 0x34, 0x56});
    }

    private static void addGeneralResponseTag(ByteBuf bodyBuf, int frameType, Integer packageNo) {
        bodyBuf.writeShort(0x23a0);
        bodyBuf.writeShort(7); // len
        bodyBuf.writeShort(packageNo);
        bodyBuf.writeByte(0);
        bodyBuf.writeShort(frameType);
        bodyBuf.writeShort(0);
    }

    private static void addUtcTimeTag(ByteBuf bodyBuf) {
        bodyBuf.writeShort(0x23a3);
        bodyBuf.writeShort(4);
        long currentTime = System.currentTimeMillis() / 1000;
        bodyBuf.writeInt((int) currentTime);
    }

    private static void addLngLatRespTag(ByteBuf bodyBuf) {
        // This return is invalid, the longitude and latitude are filled in by default 0
        bodyBuf.writeShort(0x2094);
        bodyBuf.writeShort(8);
        bodyBuf.writeInt(0); // longitude
        bodyBuf.writeInt(0); // latitude
    }

    private static void addTimeSynTag(ByteBuf bodyBuf, DeviceSession deviceSession) {
        bodyBuf.writeShort(0x2510);
        bodyBuf.writeShort(9);
        TimeZone timeZone = deviceSession.get(DeviceSession.KEY_TIMEZONE);
        int timeZoneOffset = timeZone.getRawOffset() / 3600000;
        bodyBuf.writeByte(timeZoneOffset);
        bodyBuf.writeByte(1);

        Calendar calendar = Calendar.getInstance(timeZone);
        bodyBuf.writeShort(calendar.get(Calendar.YEAR));
        bodyBuf.writeByte(calendar.get(Calendar.MONTH) + 1);
        bodyBuf.writeByte(calendar.get(Calendar.DAY_OF_MONTH));
        bodyBuf.writeByte(calendar.get(Calendar.HOUR_OF_DAY));
        bodyBuf.writeByte(calendar.get(Calendar.MINUTE));
        bodyBuf.writeByte(calendar.get(Calendar.SECOND));
    }

}
