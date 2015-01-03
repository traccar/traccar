package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class Huabao808ProtocolDecoder extends BaseProtocolDecoder {

    private static final int PLATFORM_AUTHENTICATION = 256; // Hex: 100
    private static final int TERMINAL_AUTHENTICATION = 258; // Hex: 102
    private static final int LOCATION_REPORT = 512; // Hex: 200
    private Huabao808 huabao808;

    public Huabao808ProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (buf.readByte() == -1)
            return null;

        buf.skipBytes(1);

        int type = buf.readUnsignedShort();

        huabao808 = new Huabao808();

        if (type == PLATFORM_AUTHENTICATION) {

            huabao808.register(buf);
            response(huabao808.responsePlatformAuthentication(), channel);

        } else if (type == TERMINAL_AUTHENTICATION) {

            huabao808.register(buf);
            response(huabao808.responseTerminalAuthentication(), channel);

        } else if (type == LOCATION_REPORT) {

            Position position = huabao808.extractLocationInformation(buf);
            huabao808.responseLocation(channel, LOCATION_REPORT);

            return position;
        }

        return null;
    }

    private void response(byte[] response, Channel channel) {

        if (channel == null) return;
        ChannelBuffer buffer = ChannelBuffers.directBuffer(response.length);
        buffer.writeBytes(response);
        channel.write(buffer);

    }

    class Huabao808 {

        private static final byte MESSAGE_HEAD = 126; //Hex: 7e
        private static final byte MESSAGE_END = 126; //Hex: 7e
        private static final int REGISTER_RESPONSE = 33024; //Hex: 8100
        private static final int REGISTER_RESPONSE_MSG = 19; //Hex: 13
        private static final int REGISTER_TERMINAL_RESPONSE = 32769;
        private static final byte REGISTER_ZERO = 00;
        private static final int LOCATION_RESPONSE = 33024;
        private static final int LOCATION_MSG = 5;
        private static final int LOCATION_SUCCESS = 217;
        private static final int LOCATION_SERIAL_ID = 133;
        private byte deviceId[];
        private int serialId;
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
        private int mileage;
        private short cellID;
        private byte networkSignalStrength;
        private byte battLevel;

        private long getDeviceId() {
            return bytesToLong(phoneNumber);
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
            return position;
        }

        private String getCurrentDate() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss96");
            return dateFormat.format(new Date());
        }

        public void register(ChannelBuffer buf) {
            buf.skipBytes(2);
            deviceId = buf.readBytes(6).array();
            serialId = buf.readUnsignedShort();
        }


        public byte[] responsePlatformAuthentication() throws IOException {

            ByteArrayOutputStream response = new ByteArrayOutputStream();

            response.write(MESSAGE_HEAD);
            response.write(BigInteger.valueOf(REGISTER_RESPONSE).toByteArray()[1]);
            response.write(REGISTER_ZERO);
            response.write(REGISTER_ZERO);
            response.write(REGISTER_RESPONSE_MSG);
            response.write(deviceId);
            response.write(REGISTER_ZERO);
            response.write(LOCATION_SERIAL_ID);
            response.write(REGISTER_ZERO);
            response.write(serialId);
            response.write(REGISTER_ZERO);
            response.write(getCurrentDate().getBytes());
            response.write(hash(response.toByteArray()));
            response.write(MESSAGE_END);

            return response.toByteArray();

        }


        public byte[] responseTerminalAuthentication() throws IOException {

            ByteArrayOutputStream response = new ByteArrayOutputStream();

            response.write(MESSAGE_HEAD);
            response.write(BigInteger.valueOf(REGISTER_TERMINAL_RESPONSE).toByteArray()[1]);
            response.write(1);
            response.write(REGISTER_ZERO);
            response.write(5);
            response.write(deviceId);
            response.write(REGISTER_ZERO);
            response.write(136);
            response.write(REGISTER_ZERO);
            response.write(serialId);
            response.write(1);
            response.write(2);
            response.write(REGISTER_ZERO);
            response.write(hash(response.toByteArray()));
            response.write(MESSAGE_END);
            return response.toByteArray();

        }

        public Position extractLocationInformation(ChannelBuffer buf) {

            buf.skipBytes(2);
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
            buf.skipBytes(2);
            mileage = buf.readInt();
            buf.skipBytes(8);
            cellID = buf.readShort();
            networkSignalStrength = buf.readByte();
            battLevel = buf.readByte();

            return getPosition();

        }

        public void responseLocation(Channel channel, int messageId) {

            ByteArrayOutputStream response = new ByteArrayOutputStream();

            response.write(MESSAGE_HEAD);
            response.write(BigInteger.valueOf(REGISTER_TERMINAL_RESPONSE).toByteArray()[1]);
            response.write(1);
            response.write(LOCATION_RESPONSE);
            response.write(LOCATION_MSG);
            try {
                response.write(phoneNumber);
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.write(REGISTER_ZERO);
            response.write(LOCATION_SERIAL_ID);
            response.write(REGISTER_ZERO);
            response.write(messageSerialNumber);
            response.write(2);
            response.write(REGISTER_ZERO);
            response.write(messageId);
            response.write(LOCATION_SUCCESS);
            response.write(MESSAGE_END);

            response(response.toByteArray(), channel);

        }

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

        public  byte hash(byte[] response) {

            byte result = response[1];

            for (int i = 2; i < response.length; i++) {
                result = (byte) (result ^ response[i]);
            }

            return result;

        }
    }
}
