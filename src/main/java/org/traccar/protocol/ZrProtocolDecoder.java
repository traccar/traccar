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
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author QingtaiJiang
 * @date 2023/9/18 16:54
 * @email qingtaij@163.com
 */
public class ZrProtocolDecoder extends BaseProtocolDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZrProtocolDecoder.class);

    private static final String FRAME_HEADER = "dddd";
    private static final String FRAME_TAIL = "ffff";

    private static final int FRAME_TYPE_HEARTBEAT = 0x0100;
    private static final int FRAME_TYPE_DEV_INFO = 0x0101;
    private static final int FRAME_TYPE_REPORT = 0x0102;
    private static final int FRAME_TYPE_SER_GEN_ACK = 0x0400;
    private static final int FRAME_TYPE_LBS_TRANSFER_LONG_LAT = 0x0103;
    private static final int FRAME_TYPE_LBS_TRANSFER_LONG_LAT_ACK = 0x0403;
    private static final int FRAME_TYPE_TIME_SYN = 0x0104;
    private static final int FRAME_TYPE_TIME_SYN_ACK = 0x0404;
    public static final int FRAME_TYPE_CFG = 0x0700;
    private static final int FRAME_TYPE_DEV_GEN_ACK = 0x0a00;
    public static final int FRAME_TYPE_QUERY = 0x0701;
    private static final int FRAME_TYPE_QUERY_ACK = 0x0a01;

    private static final String STATE_ACC = "stateAcc";
    private static final Integer ACC_ON = 1;
    private static final Integer ACC_OFF = 0;

    public ZrProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        // skip start marker
        buf.readShort();
        // skip msg len
        buf.readUnsignedShort();
        short editionNum = buf.readShort();
        byte encryptionType = buf.readByte();
        // Device ID
        ByteBuf id = buf.readSlice(10);
        int frameType = buf.readUnsignedShort();
        String subpackageFlag = ByteBufUtil.hexDump(buf.readSlice(1));
        Integer packageNo = buf.readUnsignedShort();
        int bodyLen = buf.readUnsignedByte();
        ByteBuf bodyBuf = buf.readSlice(bodyLen);
        byte checkSum = buf.readByte();
        // read all buf auto to release

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, decodeId(id));
        if (deviceSession == null) {
            return null;
        }

        if (!deviceSession.contains(DeviceSession.KEY_TIMEZONE)) {
            deviceSession.set(DeviceSession.KEY_TIMEZONE, getTimeZone(deviceSession.getDeviceId(), "UTC"));
        }

        switch (frameType) {
            case FRAME_TYPE_HEARTBEAT:
            case FRAME_TYPE_DEV_INFO:
                sendGeneralResponse(channel, remoteAddress, id, frameType, editionNum, encryptionType, packageNo);
                break;
            case FRAME_TYPE_REPORT:
                sendGeneralResponse(channel, remoteAddress, id, frameType, editionNum, encryptionType, packageNo);
                // GPS data
                return decodeFrameTypeReport(deviceSession, bodyBuf);
            case FRAME_TYPE_LBS_TRANSFER_LONG_LAT:
                sendLbsTransferResponse(channel, remoteAddress, id, frameType, editionNum, encryptionType, packageNo);
                break;
            case FRAME_TYPE_TIME_SYN:
                sendTimeSynResponse(channel, remoteAddress, deviceSession, id, frameType, editionNum, encryptionType, packageNo);
                break;
            case FRAME_TYPE_DEV_GEN_ACK:
            case FRAME_TYPE_QUERY_ACK:
                // nothing to do
                break;
            default:
                LOGGER.error("zr protocol decoder not support this frame type:{}", frameType);
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
                    position.setSpeed(valueBuf.readUnsignedShort());
                    position.setCourse(valueBuf.readUnsignedShort());
                    position.setAltitude(valueBuf.readInt());
                    position.setLongitude(valueBuf.readInt() * 0.000001);
                    position.setLatitude(valueBuf.readInt() * 0.000001);
                    long l = valueBuf.readUnsignedInt(); // UTC second time
                    Calendar calendar = Calendar.getInstance((TimeZone) deviceSession.get(DeviceSession.KEY_TIMEZONE));
                    calendar.setTimeInMillis(l * 1000L);
                    position.setTime(calendar.getTime());
                    break;
                case 0x240c:
                    position.set(Position.KEY_ODOMETER, valueBuf.readUnsignedInt());
                    break;
                case 0x2310:
                    long value = valueBuf.readLong();
                    position.set(Position.KEY_ALARM, decode2310(value));

                    if (BitUtil.check(value, 12)) {
                        deviceSession.set(STATE_ACC, ACC_ON);
                    }
                    if (BitUtil.check(value, 13)) {
                        deviceSession.set(STATE_ACC, ACC_OFF);
                    }
                    break;
            }
        }

        if (position.getLatitude() != 0 && position.getLongitude() != 0) {
            position.setValid(true);
        } else {
            return null;
        }

        Integer accState = deviceSession.get(STATE_ACC);
        position.set(Position.KEY_IGNITION, accState != null && accState.equals(ACC_ON));
        return position;
    }

    private String decode2310(long value) {
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
        int i = 0;
        while (i < str.length() && str.charAt(i) == '0') {
            i++;
        }
        String devId = str.substring(i);
        // if the devId is all zero return default 0
        if (devId.length() == 0) {
            return "0";
        }
        return devId;
    }

    public static ByteBuf formatMessage(int type, ByteBuf id, short editionNum, byte encryptionType, Integer packageNo, ByteBuf body) {
        ByteBuf buffer = Unpooled.buffer();

        // header
        buffer.writeBytes(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(FRAME_HEADER)));
        // Message length, please specify one at will
        buffer.writeShort(12);
        // Protocol version number
        buffer.writeShort(editionNum);
        // Encryption type
        buffer.writeByte(encryptionType);
        // Terminal device number
        buffer.writeBytes(id);
        // msg id
        buffer.writeShort(type);
        // subcontract item
        buffer.writeByte(0x11);
        // packageNo
        buffer.writeShort(packageNo);
        // body len
        buffer.writeByte(body.readableBytes());
        // body
        buffer.writeBytes(body);
        // check sum
        buffer.writeByte(12);
        // msg tail
        buffer.writeBytes(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(FRAME_TAIL)));

        // Correct message length
        buffer.setShort(2, buffer.readableBytes() - 6);
        buffer.setByte(buffer.writerIndex() - 3, bcc(buffer));

        return buffer;
    }

    private static void sendGeneralResponse(Channel channel, SocketAddress remoteAddress, ByteBuf id, int frameType, short editionNum, byte encryptionType, Integer packageNo) {
        ByteBuf response = Unpooled.buffer();
        add2391(response);
        add23A0(response, frameType, packageNo);
        add23A3(response);
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(formatMessage(FRAME_TYPE_SER_GEN_ACK, id, editionNum, encryptionType, packageNo, response), remoteAddress));
        }
    }

    private static void sendLbsTransferResponse(Channel channel, SocketAddress remoteAddress, ByteBuf id, int frameType, short editionNum, byte encryptionType, Integer packageNo) {
        ByteBuf response = Unpooled.buffer();
        add2391(response);
        add2094(response);
        add23A3(response);
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(formatMessage(FRAME_TYPE_LBS_TRANSFER_LONG_LAT_ACK, id, editionNum, encryptionType, packageNo, response), remoteAddress));
        }
    }

    private static void sendTimeSynResponse(Channel channel, SocketAddress remoteAddress, DeviceSession deviceSession, ByteBuf id, int frameType, short editionNum, byte encryptionType, Integer packageNo) {
        ByteBuf response = Unpooled.buffer();
        add2391(response);
        add2510(response, deviceSession);
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(formatMessage(FRAME_TYPE_TIME_SYN_ACK, id, editionNum, encryptionType, packageNo, response), remoteAddress));
        }
    }

    public static void add2391(ByteBuf bodyBuf) {
        bodyBuf.writeShort(0x2391);
        bodyBuf.writeShort(3);
        bodyBuf.writeBytes(ByteBufUtil.decodeHexDump("123456"));
    }

    private static void add23A0(ByteBuf bodyBuf, int frameType, Integer packageNo) {
        bodyBuf.writeShort(0x23a0);
        bodyBuf.writeShort(7); // len

        bodyBuf.writeShort(packageNo);
        bodyBuf.writeByte(0);
        bodyBuf.writeShort(frameType);
        bodyBuf.writeShort(0);
    }

    private static void add23A3(ByteBuf bodyBuf) {
        bodyBuf.writeShort(0x23a3);
        bodyBuf.writeShort(4);
        long time = System.currentTimeMillis() / 1000;
        bodyBuf.writeInt((int) time);
    }

    private static void add2094(ByteBuf bodyBuf) {
        bodyBuf.writeShort(0x2094);
        bodyBuf.writeShort(8);
        bodyBuf.writeInt(0); // longitude
        bodyBuf.writeInt(0); // latitude
    }

    private static void add2510(ByteBuf bodyBuf, DeviceSession deviceSession) {
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

    /**
     * bcc check
     */
    private static byte bcc(ByteBuf byteBuf) {
        byte cs = 0;
        int readerIndex = byteBuf.readerIndex() + 2;
        int writerIndex = byteBuf.writerIndex() + -3;
        while (readerIndex < writerIndex) cs ^= byteBuf.getByte(readerIndex++);
        return cs;
    }

}
