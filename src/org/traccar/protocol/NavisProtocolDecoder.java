/*
 * Copyright 2012 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class NavisProtocolDecoder extends BaseProtocolDecoder {

    private String prefix;
    private long deviceUniqueId, serverId;

    public NavisProtocolDecoder(NavisProtocol protocol) {
        super(protocol);
    }

    public static final int F10 = 0x01;
    public static final int F20 = 0x02;
    public static final int F30 = 0x03;
    public static final int F40 = 0x04;
    public static final int F50 = 0x05;
    public static final int F51 = 0x15;
    public static final int F52 = 0x25;

    private static boolean isFormat(int type, int... types) {
        for (int i : types) {
            if (type == i) {
                return true;
            }
        }
        return false;
    }

    private static final class ParseResult {
        private final long id;
        private final Position position;

        private ParseResult(long id, Position position) {
            this.id = id;
            this.position = position;
        }

        public long getId() {
            return id;
        }

        public Position getPosition() {
            return position;
        }
    }

    private ParseResult parsePosition(DeviceSession deviceSession, ChannelBuffer buf) {
        Position position = new Position();
        position.setProtocol(getProtocolName());

        position.setDeviceId(deviceSession.getDeviceId());

        int format;
        if (buf.getUnsignedByte(buf.readerIndex()) == 0) {
            format = buf.readUnsignedShort();
        } else {
            format = buf.readUnsignedByte();
        }
        position.set("format", format);

        long index = buf.readUnsignedInt();
        position.set(Position.KEY_INDEX, index);

        position.set(Position.KEY_EVENT, buf.readUnsignedShort());

        buf.skipBytes(6); // event time

        short armedStatus = buf.readUnsignedByte();
        position.set(Position.KEY_ARMED, armedStatus & 0x7F);
        if (BitUtil.check(armedStatus, 7)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        }
        position.set(Position.KEY_STATUS, buf.readUnsignedByte());
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());

        if (isFormat(format, F10, F20, F30)) {
            position.set(Position.KEY_OUTPUT, buf.readUnsignedShort());
        } else if (isFormat(format, F40, F50, F51, F52)) {
            position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
        }

        if (isFormat(format, F10, F20, F30, F40)) {
            position.set(Position.KEY_INPUT, buf.readUnsignedShort());
        } else if (isFormat(format, F50, F51, F52)) {
            position.set(Position.KEY_INPUT, buf.readUnsignedByte());
        }

        position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
        position.set(Position.KEY_BATTERY, buf.readUnsignedShort());

        if (isFormat(format, F10, F20, F30)) {
            position.set(Position.PREFIX_TEMP + 1, buf.readShort());
        }

        if (isFormat(format, F10, F20, F50, F52)) {
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
        }

        // Impulse counters
        if (isFormat(format, F20, F50, F51, F52)) {
            buf.readUnsignedInt();
            buf.readUnsignedInt();
        }

        if (isFormat(format, F20, F50, F51, F52)) {
            int locationStatus = buf.readUnsignedByte();
            position.setValid(BitUtil.check(locationStatus, 1));

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte() + 1, buf.readUnsignedByte());
            position.setTime(dateBuilder.getDate());

            position.setLatitude(buf.readFloat() / Math.PI * 180);
            position.setLongitude(buf.readFloat() / Math.PI * 180);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readFloat()));
            position.setCourse(buf.readUnsignedShort());

            position.set(Position.KEY_ODOMETER, buf.readFloat() * 1000);
            position.set(Position.KEY_DISTANCE, buf.readFloat());

            // Segment times
            buf.readUnsignedShort();
            buf.readUnsignedShort();
        }

        // Other
        if (isFormat(format, F51, F52)) {
            buf.readUnsignedShort();
            buf.readByte();
            buf.readUnsignedShort();
            buf.readUnsignedShort();
            buf.readByte();
            buf.readUnsignedShort();
            buf.readUnsignedShort();
            buf.readByte();
            buf.readUnsignedShort();
        }

        // Four temperature sensors
        if (isFormat(format, F40, F52)) {
            buf.readByte();
            buf.readByte();
            buf.readByte();
            buf.readByte();
        }

        return new ParseResult(index, position);
    }

    private Object processSingle(DeviceSession deviceSession, Channel channel, ChannelBuffer buf) {
        ParseResult result = parsePosition(deviceSession, buf);

        ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 8);
        response.writeBytes(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "*<T", StandardCharsets.US_ASCII));
        response.writeInt((int) result.getId());
        sendReply(channel, response);

        if (result.getPosition().getFixTime() == null) {
            return null;
        }

        return result.getPosition();
    }

    private Object processArray(DeviceSession deviceSession, Channel channel, ChannelBuffer buf) {
        List<Position> positions = new LinkedList<>();
        int count = buf.readUnsignedByte();

        for (int i = 0; i < count; i++) {
            Position position = parsePosition(deviceSession, buf).getPosition();
            if (position.getFixTime() != null) {
                positions.add(position);
            }
        }

        ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 8);
        response.writeBytes(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "*<A", StandardCharsets.US_ASCII));
        response.writeByte(count);
        sendReply(channel, response);

        if (positions.isEmpty()) {
            return null;
        }

        return positions;
    }

    private Object processHandshake(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) {
        buf.readByte(); // semicolon symbol
        if (getDeviceSession(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII)) != null) {
            sendReply(channel, ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "*<S", StandardCharsets.US_ASCII));
        }
        return null;
    }

    private static short checksum(ChannelBuffer buf) {
        short sum = 0;
        for (int i = 0; i < buf.readableBytes(); i++) {
            sum ^= buf.getUnsignedByte(i);
        }
        return sum;
    }

    private void sendReply(Channel channel, ChannelBuffer data) {
        ChannelBuffer header = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 16);
        header.writeBytes(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, prefix, StandardCharsets.US_ASCII));
        header.writeInt((int) deviceUniqueId);
        header.writeInt((int) serverId);
        header.writeShort(data.readableBytes());
        header.writeByte(checksum(data));
        header.writeByte(checksum(header));

        if (channel != null) {
            channel.write(ChannelBuffers.copiedBuffer(header, data));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        prefix = buf.toString(buf.readerIndex(), 4, StandardCharsets.US_ASCII);
        buf.skipBytes(prefix.length()); // prefix @NTC by default
        serverId = buf.readUnsignedInt();
        deviceUniqueId = buf.readUnsignedInt();
        int length = buf.readUnsignedShort();
        buf.skipBytes(2); // header and data XOR checksum

        if (length == 0) {
            return null; // keep alive message
        }

        String type = buf.toString(buf.readerIndex(), 3, StandardCharsets.US_ASCII);
        buf.skipBytes(type.length());

        if (type.equals("*>S")) {
            return processHandshake(channel, remoteAddress, buf);
        } else {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession != null) {
                if (type.equals("*>T")) {
                    return processSingle(deviceSession, channel, buf);
                } else if (type.equals("*>A")) {
                    return processArray(deviceSession, channel, buf);
                }
            }
        }

        return null;
    }

}
