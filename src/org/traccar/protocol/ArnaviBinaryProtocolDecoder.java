package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.Log;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ArnaviBinaryProtocolDecoder extends BaseProtocolDecoder {

    public ArnaviBinaryProtocolDecoder(ArnaviBinaryProtocol protocol) {
        super(protocol);
    }

    private static final int PROTOCOL_VERSION1 = 0x22;
    private static final int PROTOCOL_VERSION2 = 0x23;

    private static final byte PACKET_DATA = 0x01;
    private static final byte PACKET_TEXT = 0x03;
    private static final byte PACKET_FILE = 0x04;
    private static final byte PACKET_DATA_BINARY = 0x06;

    private static final byte TAG_VOLTAGE_SUPPLY = 1;
    private static final byte TAG_ID_KEY = 2;
    private static final byte TAG_LATITUDE = 3;
    private static final byte TAG_LONGITUDE = 4;
    private static final byte TAG_COORD_PARAMS = 5;
    private static final byte TAG_DEVICE_STATUS = 9;
    private static final byte TAG_FREQ_0 = 20;
    private static final byte TAG_FREQ_1 = 21;
    private static final byte TAG_FREQ_2 = 22;
    private static final byte TAG_FREQ_3 = 23;
    private static final byte TAG_FREQ_4 = 24;
    private static final byte TAG_FREQ_5 = 25;
    private static final byte TAG_FREQ_6 = 26;
    private static final byte TAG_FREQ_7 = 27;
    private static final byte TAG_VOLT_0 = 30;
    private static final byte TAG_VOLT_1 = 31;
    private static final byte TAG_VOLT_2 = 32;
    private static final byte TAG_VOLT_3 = 33;
    private static final byte TAG_VOLT_4 = 34;
    private static final byte TAG_VOLT_5 = 35;
    private static final byte TAG_VOLT_6 = 36;
    private static final byte TAG_VOLT_7 = 37;
    private static final byte TAG_LLS_0 = 70;
    private static final byte TAG_LLS_1 = 71;
    private static final byte TAG_LLS_2 = 72;
    private static final byte TAG_LLS_3 = 73;
    private static final byte TAG_LLS_4 = 74;
    private static final byte TAG_LLS_5 = 75;
    private static final byte TAG_LLS_6 = 76;
    private static final byte TAG_LLS_7 = 77;
    private static final byte TAG_LLS_8 = 78;
    private static final byte TAG_LLS_9 = 79;


    private List<Position> positions;


    private static int crc8(byte[] data) {
        byte crc = 0;
        for (byte b : data) {
            crc += b;
        }
        return (crc & 0xff);
    }

    private void sendConfirmationHeader(Channel channel, int headerVersion) {
        if (headerVersion != PROTOCOL_VERSION1 && headerVersion != PROTOCOL_VERSION2) {
            return;
        }
        ChannelBuffer reply;
        if (headerVersion == PROTOCOL_VERSION1) {
            reply = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 4);
            reply.writeInt(0x7B00007D);
        } else {
            byte[] timestampBytes = ByteBuffer.allocate(4).putInt((int) Instant.now().getEpochSecond()).array();
            reply = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 9);
            reply.writeByte((byte) 0x7B);
            reply.writeByte((byte) 0x04);
            reply.writeByte((byte) 0x00);
            reply.writeByte((byte) crc8(timestampBytes));
            reply.writeBytes(timestampBytes);
            reply.writeByte((byte) 0x7D);
        }
        if (channel != null) {
            channel.write(reply);
        }
    }

    private void sendConfirmationPackage(Channel channel, int packageCounter) {
        if (channel == null) {
            return;
        }
        ChannelBuffer reply = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 4);
        byte[] replBytes = new byte[]{0x7B, 0x00, (byte) packageCounter, 0x7D};
        reply.writeBytes(replBytes);
        channel.write(reply);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        int length = buf.readableBytes();

        if (length == 10) {
            short signature = buf.readUnsignedByte(); // header
            if (signature != 0xff) {
                return null;
            }

            short protocolVersion = buf.readUnsignedByte(); // protocol version. Must be 0x22 or 0x23
            if (protocolVersion != PROTOCOL_VERSION1 && protocolVersion != PROTOCOL_VERSION2) {
                return null;
            }

            String imei = String.valueOf(buf.readLong());
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            sendConfirmationHeader(channel, protocolVersion);

        } else {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession != null) {
                positions = new LinkedList<>();
                short signature = buf.readUnsignedByte(); // header of package

                if (signature == 0x5B) {
                    // standart package signature
                    byte[] currentPackage = new byte[length - 1];
                    buf.readBytes(currentPackage);
                    parsePackage(currentPackage, channel, deviceSession);
                }

                return positions;
            } else {
                System.out.println("deviceSession is null while PACKAGE got!");
            }
        }
        System.out.println("decode return null\n");
        return null;
    }

    private void parsePackage(byte[] packageRawData, Channel channel, DeviceSession deviceSession) {
        int packageCounter = 0xFE;                                       // package has not been tested by default
        int packageLength = packageRawData.length;
        ChannelBuffer packageBuffer = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, packageLength);
        packageBuffer.writeBytes(packageRawData);

        packageCounter = packageBuffer.readUnsignedByte();

        // start parsing PACKAGE
        while (packageBuffer.readerIndex() < packageLength - 1) {
            short signature = packageBuffer.readUnsignedByte();                         // header of packet
            int packetLength = packageBuffer.readUnsignedShort();                       // Length of current packet

            byte[] currentPacketWTime = new byte[packetLength + 4];
            packageBuffer.readBytes(currentPacketWTime);

            byte[] currentPacket = Arrays.copyOfRange(currentPacketWTime, 4, packetLength + 4);
            int packetCRC = packageBuffer.readUnsignedByte();
            if (packetCRC != crc8(currentPacketWTime)) {
                Log.error(String.format("CRC packet error! Got %d, instead calculated %d",
                        packetCRC, crc8(currentPacketWTime)));
                return;
            }

            long timestamp = ByteBuffer.wrap(Arrays.copyOfRange(currentPacketWTime, 0, 4))
                    .order(ByteOrder.LITTLE_ENDIAN).getInt();
            Date packetUnixtime = new Date(timestamp * 1000);

            switch (signature) {
                case PACKET_DATA:
                    Position position = parsePacket(currentPacket);
                    position.setDeviceId(deviceSession.getDeviceId());
                    position.setProtocol(getProtocolName());
                    position.setTime(packetUnixtime);
                    positions.add(position);
                    break;
                default:
//                    System.out.printf("Got packet type: %s, length: %d. Skipped..\n",
//                            Integer.toHexString(signature), packetLength);
                    break;
            }
        }
        short endPackageSignature = packageBuffer.readUnsignedByte();
        if (endPackageSignature != 0x5D) {
            return;
        }
        sendConfirmationPackage(channel, packageCounter);
    }

    private Position parsePacket(byte[] packetRawData) {
        int packetLength = packetRawData.length;
        ChannelBuffer packetBuffer = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, packetLength);
        packetBuffer.writeBytes(packetRawData);

        boolean hasLocation = false;
        Position position = new Position();

        while (packetBuffer.readerIndex() < packetLength - 1) {
            short tagId = packetBuffer.readUnsignedByte();
            int tagValue = packetBuffer.readInt();

            switch (tagId) {
                case TAG_LATITUDE:
                    position.setLatitude(Float.intBitsToFloat(tagValue));
                    hasLocation = true;
                    break;
                case TAG_LONGITUDE:
                    position.setLongitude(Float.intBitsToFloat(tagValue));
                    hasLocation = true;
                    break;
                case TAG_COORD_PARAMS:
                    position.setSpeed((double) (tagValue >> 24) * 1.852);
                    position.set(Position.KEY_SATELLITES, (double) (tagValue >> 20 & 0x0F));
                    position.setAltitude((double) ((tagValue >> 8 & 0xFF) * 10));
                    position.setCourse((double) ((tagValue & 0xFF) * 2));
                    break;
                case TAG_VOLT_0:
                case TAG_VOLT_1:
                case TAG_VOLT_2:
                case TAG_VOLT_3:
                case TAG_VOLT_4:
                case TAG_VOLT_5:
                case TAG_VOLT_6:
                case TAG_VOLT_7:
                    position.set(Position.PREFIX_ADC + (tagId - TAG_VOLT_0 + 1), tagValue);
                    break;
                case TAG_FREQ_0:
                case TAG_FREQ_1:
                case TAG_FREQ_2:
                case TAG_FREQ_3:
                case TAG_FREQ_4:
                case TAG_FREQ_5:
                case TAG_FREQ_6:
                case TAG_FREQ_7:
                    position.set(Position.PREFIX_COUNT + (tagId - TAG_FREQ_0 + 1), tagValue);
                    break;
                case TAG_LLS_0:
                case TAG_LLS_1:
                case TAG_LLS_2:
                case TAG_LLS_3:
                case TAG_LLS_4:
                case TAG_LLS_5:
                case TAG_LLS_6:
                case TAG_LLS_7:
                case TAG_LLS_8:
                case TAG_LLS_9:
                    position.set(Position.PREFIX_IO + (tagId - TAG_LLS_0 + 1), tagValue);
                    break;
                case TAG_VOLTAGE_SUPPLY:
                    position.set(Position.KEY_POWER, (int) tagValue >> 16);
                    position.set(Position.KEY_BATTERY, (int) tagValue & 0xFFFF);
                default:
                    System.out.printf("Unknown tag: %d, value: %d\n", tagId, tagValue);
            }
        }
        position.setServerTime(new Date());
        return position;
    }
}
