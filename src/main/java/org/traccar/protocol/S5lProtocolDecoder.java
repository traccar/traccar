package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Device;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

public class S5lProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(S5lProtocolDecoder.class);

    public S5lProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_LOCATION = 0x12;
    public static final int MSG_STATUS = 0x13; // HEATBEAT PACKET
    public static final int MSG_STRING_INFO = 0x15;
    public static final int MSG_ALARM = 0x16;
    public static final int MSG_COMMAND = 0x80;

    private void sendResponse(Channel channel, int type, int index, ByteBuf content) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            int length = 5 + (content != null ? content.readableBytes() : 0);
            response.writeShort(0x7878);
            response.writeByte(length);
            response.writeByte(type);
            if (content != null) {
                response.writeBytes(content);
                content.release();
            }
            response.writeShort(index);
            response.writeShort(Checksum.crc16(Checksum.CRC16_X25,
                    response.nioBuffer(2, response.writerIndex() - 2)));
            response.writeByte('\r');
            response.writeByte('\n');
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readShort(); // drop the start marker 0x7878
        int length = buf.readUnsignedByte();
        int dataLength = length - 5;
        int type = buf.readUnsignedByte();

        Position position = new Position(getProtocolName());
        DeviceSession deviceSession = null;

        position.set(Position.KEY_TYPE, type);

        if (type != MSG_LOGIN) {
            deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());
            if (!deviceSession.contains(DeviceSession.KEY_TIMEZONE)) {
                deviceSession.set(DeviceSession.KEY_TIMEZONE, getTimeZone(deviceSession.getDeviceId()));
            }
        }

        if (type == MSG_LOGIN) {

            String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
            buf.readUnsignedShort(); // type

            deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession != null) {
                TimeZone timeZone = getTimeZone(deviceSession.getDeviceId(), null);
                if (timeZone == null && dataLength > 10) {
                    int extensionBits = buf.readUnsignedShort();
                    int hours = (extensionBits >> 4) / 100;
                    int minutes = (extensionBits >> 4) % 100;
                    int offset = (hours * 60 + minutes) * 60;
                    if ((extensionBits & 0x8) != 0) {
                        offset = -offset;
                    }
                    timeZone = TimeZone.getTimeZone("UTC");
                    timeZone.setRawOffset(offset * 1000);
                }
                deviceSession.set(DeviceSession.KEY_TIMEZONE, timeZone);

                sendResponse(channel, type, buf.getShort(buf.writerIndex() - 6), null);
            }

            return null;

        } else if (type == MSG_STRING_INFO) { // To Get command Result

            getLastLocation(position, null);

            int commandLength = buf.readUnsignedByte();

            if (commandLength > 0) {
                buf.readUnsignedInt(); // server flag (reserved)
                String data = buf.readSlice(commandLength - 4).toString(StandardCharsets.US_ASCII);
                if (data.startsWith("<ICCID:")) {
                    position.set(Position.KEY_ICCID, data.substring(7, 27));
                } else {
                    position.set(Position.KEY_RESULT, data);
                }
            }

        } else if (type == MSG_LOCATION) {

            decodeGps(position, buf, deviceSession.get(DeviceSession.KEY_TIMEZONE));
            decodeLbs(position, buf);
            decodeStatusInfo(position, buf, false);
            decodeExpandInfo(position, buf);

        } else if (type == MSG_STATUS) { // HEATBEAT PACKET

            getLastLocation(position, null);
            decodeStatusInfo(position, buf, false);

        } else if (type == MSG_ALARM) {
            decodeGps(position, buf, deviceSession.get(DeviceSession.KEY_TIMEZONE));
            decodeLbs(position, buf);
            decodeStatusInfo(position, buf, true);
            decodeExpandInfo(position, buf);

        } else {
            LOGGER.info("UNSUPPORTED DECODE TRIGGERED: {}", type);
            sendResponse(channel, type, buf.getShort(buf.writerIndex() - 6), null);
            return null; // Unsupported message type
        }
        sendResponse(channel, type, buf.getShort(buf.writerIndex() - 6), null);
        return position;
    }

    private void decodeGps(Position position, ByteBuf buf, TimeZone timezone) {

        DateBuilder dateBuilder = new DateBuilder(timezone)
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_SATELLITES, BitUtil.to(buf.readUnsignedByte(), 4)); // gps info contains more data.

        double latitude = buf.readUnsignedInt() / 60.0 / 30000.0;
        double longitude = buf.readUnsignedInt() / 60.0 / 30000.0;

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

        int courseStatus = buf.readUnsignedShort();

        position.setCourse(BitUtil.to(courseStatus, 10));
        position.setValid(BitUtil.check(courseStatus, 12));

        if (!BitUtil.check(courseStatus, 10)) {
            latitude = -latitude;
        }
        if (BitUtil.check(courseStatus, 11)) {
            longitude = -longitude;
        }

        position.setLatitude(latitude);
        position.setLongitude(longitude);

    }

    private void decodeLbs(Position position, ByteBuf buf) {
        int mcc = buf.readUnsignedShort(); // MCC
        int mnc = buf.readUnsignedByte(); // MNC
        int lac = buf.readUnsignedShort(); // LAC
        int cellId = buf.readUnsignedMedium(); // Cell ID

        position.setNetwork(new Network(CellTower.from(BitUtil.to(mcc, 15), mnc, lac, cellId)));
    }

    private void decodeStatusInfo(Position position, ByteBuf buf, boolean isAlarm) {
        // decode Terminal Information
        int terminalInfo = buf.readUnsignedByte();
        position.set(Position.KEY_STATUS, terminalInfo);
        position.set(Position.KEY_IGNITION, BitUtil.check(terminalInfo, 1));
        position.set(Position.KEY_CHARGE, BitUtil.check(terminalInfo, 2));
        position.set(Position.KEY_BLOCKED, BitUtil.check(terminalInfo, 7));

        if (isAlarm) {
            position.addAlarm(decodeAlarm(BitUtil.between(terminalInfo, 3, 6)));
        }

        // decode Power Information
        int powerInfo = buf.readUnsignedShort();
        position.set(Position.KEY_POWER, powerInfo / 100.0);

        // decode GSM SIGNAL
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());

        // Alarm Expansion bit
        int alarmExpansion = buf.readUnsignedByte();
        if (isAlarm && alarmExpansion == 0x02) {
            position.addAlarm(Position.ALARM_POWER_CUT);
        }

    }

    private void decodeExpandInfo(Position position, ByteBuf buf) {

        // decode Oil Data (Fuel Information)
        int oilData = buf.readUnsignedShort();
        position.set("oil", oilData);

        // get device
        Device device = getCacheManager().getObject(Device.class, position.getDeviceId());
        if (device != null) {
            int fuelMax = device.getInteger("FuelMax");
            if (fuelMax > 0) {
                position.set("FuelLevel", String.valueOf(oilData / (double) fuelMax * 100) + "%");
            }
        }

        // decode Temperature Data
        int temperature = buf.readUnsignedByte();
        if (BitUtil.check(temperature, 7)) {
            temperature = -BitUtil.to(temperature, 7);
        }
        position.set(Position.PREFIX_TEMP + 1, temperature);

        // decode mileage data(meters)
        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 10);

    }

    private String decodeAlarm(int value) {
        return switch (value) {
            case 0x01 -> Position.ALARM_VIBRATION;
            case 0x02 -> Position.ALARM_POWER_CUT;
            case 0x03 -> Position.ALARM_LOW_POWER;
            case 0x04 -> Position.ALARM_SOS;
            case 0x06 -> Position.ALARM_GEOFENCE;
            case 0x07 -> Position.ALARM_OVERSPEED;
            default -> null;
        };
    }
}
