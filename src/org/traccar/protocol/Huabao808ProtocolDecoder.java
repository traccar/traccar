package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class Huabao808ProtocolDecoder extends BaseProtocolDecoder {

    private Huabao808 huabao808;

    public Huabao808ProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (buf.readByte() == -1) return null;

        int type = buf.readUnsignedShort();

        huabao808 = new Huabao808();

        if (type == MessageId.PLATFORM_AUTHENTICATION.getId()) {

            huabao808.register(buf);
            response(responsePlatformAuthentication(), channel);

        } else if (type == MessageId.TERMINAL_AUTHENTICATION.getId()) {

            huabao808.register(buf);
            response(responseTerminalAuthentication(), channel);

        } else if (type == MessageId.LOCATION_REPORT.getId()) {

            Position position = huabao808.extractLocationInformation(buf);
            response(responseLocation(), channel);

            return position;
        }

        return null;
    }

    public byte[] responseTerminalAuthentication()  throws Exception {

        if(huabao808 == null) return new byte[]{};

        return huabao808.responseTerminalAuthentication();
    }

    public byte[] responseLocation()  throws Exception {

        if(huabao808 == null) return new byte[]{};

        return huabao808.responseLocation();
    }

    public byte[] responsePlatformAuthentication() throws Exception {

        if(huabao808 == null) return new byte[]{};

        return huabao808.responsePlatformAuthentication();
    }

    private void response(byte[] response, Channel channel) {

        if (channel == null) return;
        ChannelBuffer buffer = ChannelBuffers.directBuffer(response.length);
        buffer.writeBytes(response);
        channel.write(buffer);
    }

    enum OperationStatus {

        SUCCESS(0);

        private int status;

        OperationStatus(int status) {
            this.status = status;
        }

        int getStatus() {
            return status;
        }

        byte[] asResponse() {
            return BigInteger.valueOf(status).toByteArray();
        }
    }

    enum MessageBodyNature {

        PLATFORM(19),
        TERMINAL(5),
        LOCALIZATION(5);

        private int status;

        MessageBodyNature(int status) {
            this.status = status;
        }

        int getStatus() {
            return status;
        }

        byte[] asResponse() {
            return new byte[]{0, BigInteger.valueOf(status).byteValue()};
        }
    }

    enum MessageId {

        PLATFORM_AUTHENTICATION(256),
        TERMINAL_AUTHENTICATION(258),
        LOCATION_REPORT(512),

        PLATFORM_RESPONSE(33024),
        TERMINAL_RESPONSE(32769),
        LOCATION_RESPONSE(32769);

        private int id;

        MessageId(int id) {
            this.id = id;
        }

        int getId() {
            return id;
        }

        byte[] asResponse() {
            return BigInteger.valueOf(id).toByteArray();
        }
    }

    enum SerialId {

        PLATFORM_SERIAL_RESPONSE(133),
        TERMINAL_SERIAL_RESPONSE(136),
        LOCATION_SERIAL_RESPONSE(133);

        private int serial;

        SerialId(int serial) {
            this.serial = serial;
        }

        byte[] asResponse() {
            return BigInteger.valueOf(serial).toByteArray();
        }
    }

    class Huabao808 {
        private static final byte MESSAGE_HEAD = 126;               //Hex: 7e
        private static final byte MESSAGE_END = 126;                //Hex: 7e

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

            String imei = bcdtoString(phoneNumber);
            try {
                return getDataManager().getDeviceByImei(imei).getId();
            } catch (Exception e) {
                Log.warning("Unknown device - " + imei);
            }

            return -1;
        }

        private double getLatitude() {
            double result = latitude / (60.0 * 30000.0);
            if ((direction & 0x0400) == 0) result = -result;
            return result;
        }

        private double getLongitude() {
            double result = longitude / (60.0 * 30000.0);
            if ((direction & 0x0800) == 0) result = -result;
            return result;
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

            Date result = new Date();

            SimpleDateFormat format = new SimpleDateFormat("yyMMddhhmmss");

            try {
                result = format.parse(bcdtoString(time));
            } catch (ParseException e) {
                Log.warning(String.format("%s is not a valid date (yyMMddhhmmss)", bcdtoString(time)));
            }
            return result;
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
            response.write(MessageId.PLATFORM_RESPONSE.asResponse(), 1, 2);
            response.write(MessageBodyNature.PLATFORM.asResponse());
            response.write(deviceId);
            response.write(SerialId.LOCATION_SERIAL_RESPONSE.asResponse());
            response.write(serialAsResponse(serialId));
            response.write(OperationStatus.SUCCESS.asResponse());
            response.write(getCurrentDate().getBytes());
            response.write(hash(response.toByteArray()));
            response.write(MESSAGE_END);

            return response.toByteArray();
        }

        public byte[] responseTerminalAuthentication() throws IOException {

            ByteArrayOutputStream response = new ByteArrayOutputStream();

            response.write(MESSAGE_HEAD);
            response.write(MessageId.TERMINAL_RESPONSE.asResponse(), 1, 2);
            response.write(MessageBodyNature.TERMINAL.asResponse());
            response.write(deviceId);
            response.write(SerialId.TERMINAL_SERIAL_RESPONSE.asResponse());
            response.write(serialAsResponse(serialId));
            response.write(MessageId.TERMINAL_AUTHENTICATION.asResponse());
            response.write(OperationStatus.SUCCESS.asResponse());
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
                latitude = buf.readUnsignedInt();
                longitude = buf.readUnsignedInt();
                altitude = buf.readShort();
                speed = buf.readShort();
                direction = buf.readShort();
                time = buf.readBytes(6).array();
                buf.skipBytes(2);
                mileage = buf.readInt();
                if(buf.array().length >=61) {
                    buf.skipBytes(8);
                    cellID = buf.readShort();
                    networkSignalStrength = buf.readByte();
                    battLevel = buf.readByte();
                }

            return getPosition();
        }

        public byte[] responseLocation() throws IOException {

            ByteArrayOutputStream response = new ByteArrayOutputStream();

            response.write(MESSAGE_HEAD);
            response.write(MessageId.LOCATION_RESPONSE.asResponse(), 1, 2);
            response.write(MessageBodyNature.LOCALIZATION.asResponse());
            response.write(phoneNumber);
            response.write(SerialId.LOCATION_SERIAL_RESPONSE.asResponse());
            response.write(serialAsResponse(messageSerialNumber));
            response.write(MessageId.LOCATION_REPORT.asResponse());
            response.write(OperationStatus.SUCCESS.asResponse());
            response.write(hash(response.toByteArray()));
            response.write(MESSAGE_END);

            return response.toByteArray();
        }

        public byte hash(byte[] response) {

            byte result = response[1];

            for (int i = 2; i < response.length; i++) {
                result = (byte) (result ^ response[i]);
            }

            return result;
        }

        public String bcdtoString(byte bcd) {

            StringBuilder sb = new StringBuilder();

            byte high = (byte) (bcd & 0xf0);

            high >>>= (byte) 4;
            high = (byte) (high & 0x0f);

            byte low = (byte) (bcd & 0x0f);

            sb.append(high);
            sb.append(low);

            return sb.toString();
        }

        public String bcdtoString(byte[] bcd) {

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bcd.length; i++) {
                sb.append(bcdtoString(bcd[i]));
            }

            return sb.toString();
        }

        byte[] serialAsResponse(int serial) {
            return new byte[]{0, BigInteger.valueOf(serial).byteValue()};
        }

    }
}
