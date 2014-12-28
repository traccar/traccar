package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

public class Huabao808ProtocolDecoder extends BaseProtocolDecoder {

    private static final int PLATFORM_AUTHENTICATION = 256;
    private static final int TERMINAL_AUTHENTICATION = 258;
    private static final int LOCATION_REPORT = 512;
    public Huabao808ProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (buf == null)
            return null;

        byte head = buf.readByte();

        if (head != -1) {

            Huabao808 huabao808 = new Huabao808();

            int type = buf.readUnsignedShort();

            if (type == PLATFORM_AUTHENTICATION) {

                huabao808.register(buf, PLATFORM_AUTHENTICATION);
                huabao808.responsePlatformAuthentication(channel);


            } else if (type == TERMINAL_AUTHENTICATION) {

                huabao808.register(buf, TERMINAL_AUTHENTICATION);
                huabao808.responseTerminalAuthentication(channel);

            } else if (type == LOCATION_REPORT) {

                Position position = huabao808.extractLocationInformation(buf);
                huabao808.responseLocation(channel, LOCATION_REPORT);

                return position;
            }
        }

        return null;
    }

    class ByteUtils {

        public String bytesToString(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                byte b = bytes[i];
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        }

        public long bytesToLong(byte[] bytes) {
            return Long.parseLong(bytesToString(bytes));
        }
    }

    class Huabao808 {

        private static final byte MESSAGE_HEAD = 126;
        private static final byte MESSAGE_END = 126;
        private static final int REGISTER_RESPONSE = 33024;
        private static final int REGISTER_TERMINAL_RESPONSE = 32769;
        private static final int REGISTER_TERMINAL_SERIAL_ID = 136;
        private static final int REGISTER_SERIAL_ID = 133;
        private static final byte REGISTER_SUCCESS = 00;
        private static final int LOCATION_RESPONSE = 33024;
        private static final int LOCATION_MSG = 5;
        private static final int LOCATION_SUCCESS = 217;
        private static final int LOCATION_SERIAL_ID = 133;
        private ByteUtils byteUtils = new ByteUtils();
        private int messageId;
        private byte deviceId[];
        private int serialId;
        private int messageHash;
        private byte calibration;
        private int messageProperty;
        private byte phoneNumber[];
        private int messageSerialNumber;
        private long alarmWord;
        private long stateWord;
        private long longitude;
        private long latitude;
        private short altitude;
        private short speed;
        private short direction;
        private byte time[];
        private byte additionalDataIdTime;
        private byte additionalDataLengthTime;
        private int mileage;
        private byte additionalDataIdMileage;
        private byte additionalDataLengthMileage;
        private long mccCode;
        private byte mncCode;
        private short lac;
        private short cellID;
        private byte networkSignalStrength;
        private byte battLevel;

        private long getId() {
            return new Random().nextLong();
        }

        private long getDeviceId() {
            return byteUtils.bytesToLong(phoneNumber);
        }

        private double getLatitude() {
            return latitude / (60.0 * 30000.0);
        }

        private double getLongitude() {
            return longitude / (60.0 * 30000.0);
        }

        private double getAltitude() {
            return altitude;
        }

        private double getSpeed() {
            return speed;
        }

        private double getCourse() {
            return direction;
        }

        private String getAddress() {
            return "";
        }

        private String getExtendedInfo() {
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
            extendedInfo.set("alarm", alarmWord);
            extendedInfo.set("battery_level", battLevel);
            extendedInfo.set("mileage", mileage);
            extendedInfo.set("phone_number", mileage);
            extendedInfo.set("state", stateWord);
            extendedInfo.set("cell_id", cellID);
            extendedInfo.set("network_signal_strength", networkSignalStrength);
            return extendedInfo.toString();
        }

        private Date getTime() {
            return null;
        }

        private Position getPosition() {
            Position position = new Position();
            position.setId(getId());
            position.setDeviceId(getDeviceId());
            position.setServerTime(new Date());
            position.setExtendedInfo(getExtendedInfo());
            position.setTime(getTime());
            position.setValid(Boolean.TRUE);
            position.setLatitude(getLatitude());
            position.setLongitude(getLongitude());
            position.setAltitude(getAltitude());
            position.setSpeed(getSpeed());
            position.setCourse(getCourse());
            position.setAddress(getAddress());
            return position;
        }

        private String getCurrentDate() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss96");
            return dateFormat.format(new Date());
        }

        public void register(ChannelBuffer buf, int messageId) {
            this.messageId = messageId;
            messageHash = buf.readUnsignedShort();
            deviceId = buf.readBytes(6).array();
            serialId = buf.readUnsignedShort();
            buf.readBytes(37);
            calibration = buf.readByte();
        }

        public void responsePlatformAuthentication(Channel channel) throws IOException {
            ChannelBuffer response = ChannelBuffers.directBuffer(34);
            response.writeByte(MESSAGE_HEAD);
            response.writeShort(REGISTER_RESPONSE);
            response.writeByte(REGISTER_SUCCESS);
            response.writeByte(Byte.valueOf(calibration).intValue() ^ messageHash);
            response.writeBytes(deviceId);
            response.writeShort(REGISTER_SERIAL_ID);
            response.writeShort(serialId);
            response.writeByte(REGISTER_SUCCESS);
            response.writeBytes(getCurrentDate().getBytes());
            response.writeByte(calibration);
            response.writeByte(MESSAGE_END);
            channel.write(response);

        }

        public void responseTerminalAuthentication(Channel channel) throws IOException {
            ChannelBuffer response = ChannelBuffers.directBuffer(20);
            response.writeByte(MESSAGE_HEAD);
            response.writeShort(REGISTER_TERMINAL_RESPONSE);
            response.writeByte(REGISTER_SUCCESS);
            response.writeByte(Byte.valueOf(calibration).intValue() ^ messageHash);
            response.writeBytes(deviceId);
            response.writeShort(REGISTER_TERMINAL_SERIAL_ID);
            response.writeShort(serialId);
            response.writeShort(messageId);
            response.writeByte(REGISTER_SUCCESS);
            response.writeByte(calibration);
            response.writeByte(MESSAGE_END);
            channel.write(response);
        }

        public Position extractLocationInformation(ChannelBuffer buf) {

            messageProperty = buf.readUnsignedShort();
            phoneNumber = buf.readBytes(6).array();
            messageSerialNumber = buf.readUnsignedShort();
            alarmWord = buf.readUnsignedInt();
            stateWord = buf.readUnsignedInt();
            longitude = buf.readUnsignedInt();
            latitude = buf.readUnsignedInt();
            altitude = buf.readShort();
            speed = buf.readShort();
            direction = buf.readShort();
            time = buf.readBytes(6).array();
            additionalDataIdTime = buf.readByte();
            additionalDataLengthTime = buf.readByte();
            mileage = buf.readInt();
            additionalDataIdMileage = buf.readByte();
            additionalDataLengthMileage = buf.readByte();
            mccCode = buf.readUnsignedMedium();
            mncCode = buf.readByte();
            lac = buf.readShort();
            cellID = buf.readShort();
            networkSignalStrength = buf.readByte();
            battLevel = buf.readByte();
            calibration = buf.readByte();

            return getPosition();
        }

        public void responseLocation(Channel channel, int messageId) {
            ChannelBuffer response = ChannelBuffers.directBuffer(20);
            response.writeByte(MESSAGE_HEAD);
            response.writeShort(LOCATION_RESPONSE);
            response.writeShort(LOCATION_MSG);
            response.writeBytes(phoneNumber);
            response.writeShort(LOCATION_SERIAL_ID);
            response.writeShort(messageSerialNumber);
            response.writeShort(messageId);
            response.writeShort(LOCATION_SUCCESS);
            response.writeByte(MESSAGE_END);
            channel.write(response);
        }

    }
}
