package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.traccar.protocol.Arnavi4FrameDecoder.*;

/**
 * Created by Ivan Muratov @binakot on 11.07.2017.
 */
public class Arnavi4ProtocolDecoder extends BaseProtocolDecoder {

    private static final byte RECORD_PING = 0x00;
    private static final byte RECORD_DATA = 0x01;
    private static final byte RECORD_TEXT = 0x03;
    private static final byte RECORD_FILE = 0x04;
    private static final byte RECORD_BINARY = 0x06;

    private static final byte TAG_LATITUDE = 3;
    private static final byte TAG_LONGITUDE = 4;
    private static final byte TAG_COORD_PARAMS = 5;

    public Arnavi4ProtocolDecoder(Arnavi4Protocol protocol) {
        super(protocol);
    }

    private static int modulo256Checksum(byte[] bytes) {
        int sum = 0;
        for (byte b : bytes) {
            sum = (sum + b) & 0xFF;
        }
        return sum;
    }

    private Position decodePosition(DeviceSession deviceSession, ChannelBuffer buf, long timestamp) {

        final Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(new Date(timestamp));

        while (buf.readableBytes() > 0) {
            short tagId = buf.readUnsignedByte();
            int tagValue = buf.readInt();
            switch (tagId) {
                case TAG_LATITUDE:
                    position.setLatitude(Float.intBitsToFloat(tagValue));
                    position.setValid(true);
                    break;

                case TAG_LONGITUDE:
                    position.setLongitude(Float.intBitsToFloat(tagValue));
                    position.setValid(true);
                    break;

                case TAG_COORD_PARAMS:
                    position.setSpeed((tagValue >> 24) * 1.852);
                    position.set(Position.KEY_SATELLITES, (tagValue >> 16 & 0x0F) + (tagValue >> 20 & 0x0F));
                    position.setAltitude((tagValue >> 8 & 0xFF) * 10.0);
                    position.setCourse((tagValue & 0xFF) * 2.0);
                    break;

                default:
                    break; // Skip other tags
            }
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        byte startSign = buf.readByte();

        if (startSign == HEADER_START_SIGN) {

            byte version = buf.readByte();

            String imei = String.valueOf(buf.readLong());
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

            if (deviceSession != null && channel != null) {

                final ChannelBuffer response;

                if (version == HEADER_VERSION_1) {
                    response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 4);
                    response.writeBytes(new byte[]{0x7B, 0x00, 0x00, 0x7D});

                } else if (version == HEADER_VERSION_2) {
                    response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 9);
                    response.writeBytes(new byte[]{0x7B, 0x04, 0x00});
                    byte[] timestampBytes = ByteBuffer.allocate(4).putInt((int) (System.currentTimeMillis() / 1000)).array();
                    response.writeByte(modulo256Checksum(timestampBytes));
                    response.writeBytes(timestampBytes);
                    response.writeByte(0x7D);

                } else {
                    throw new IllegalArgumentException("unsupported header version");
                }

                channel.write(response);
            }

            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (startSign == PACKAGE_START_SIGN) {

            List<Position> positions = new LinkedList<>();

            int parcelNumber = buf.readUnsignedByte();

            byte recordStartSign = buf.readByte();
            while (recordStartSign != PACKAGE_END_SIGN) {
                switch (recordStartSign) {
                    case RECORD_PING:
                    case RECORD_DATA:
                    case RECORD_TEXT:
                    case RECORD_FILE:
                    case RECORD_BINARY: {
                        int length = buf.readUnsignedShort();
                        long timestamp = buf.readUnsignedInt() * 1000;
                        ChannelBuffer recordBuf = buf.readBytes(length);

                        if (recordStartSign == RECORD_DATA) {
                            positions.add(decodePosition(deviceSession, recordBuf, timestamp));
                        }

                        buf.readUnsignedByte(); // crc

                        break;
                    }

                    default:
                        throw new IllegalArgumentException("unsupported record type");
                }

                recordStartSign = buf.readByte();
            }

            if (channel != null) {
                final ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 4);
                response.writeBytes(new byte[]{0x7B, 0x00, (byte) parcelNumber, 0x7D});
                channel.write(response);
            }

            return positions;
        }

        return null;
    }

}
