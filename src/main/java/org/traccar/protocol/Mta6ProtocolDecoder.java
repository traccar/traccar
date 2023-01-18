/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Mta6ProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mta6ProtocolDecoder.class);

    private final boolean simple;

    public Mta6ProtocolDecoder(Protocol protocol, boolean simple) {
        super(protocol);
        this.simple = simple;
    }

    private void sendContinue(Channel channel) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
    }

    private void sendResponse(Channel channel, short packetId, short packetCount) {
        ByteBuf begin = Unpooled.copiedBuffer("#ACK#", StandardCharsets.US_ASCII);
        ByteBuf end = Unpooled.buffer(3);
        end.writeByte(packetId);
        end.writeByte(packetCount);
        end.writeByte(0);

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(begin, end));
        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
    }

    private static class FloatReader {

        private int previousFloat;

        public float readFloat(ByteBuf buf) {
            switch (buf.getUnsignedByte(buf.readerIndex()) >> 6) {
                case 0:
                    previousFloat = buf.readInt() << 2;
                    break;
                case 1:
                    previousFloat = (previousFloat & 0xffffff00) + ((buf.readUnsignedByte() & 0x3f) << 2);
                    break;
                case 2:
                    previousFloat = (previousFloat & 0xffff0000) + ((buf.readUnsignedShort() & 0x3fff) << 2);
                    break;
                case 3:
                    previousFloat = (previousFloat & 0xff000000) + ((buf.readUnsignedMedium() & 0x3fffff) << 2);
                    break;
                default:
                    LOGGER.warn("MTA6 float decoding error", new IllegalArgumentException());
                    break;
            }
            return Float.intBitsToFloat(previousFloat);
        }

    }

    private static class TimeReader extends FloatReader {

        private long weekNumber;

        public Date readTime(ByteBuf buf) {
            long weekTime = (long) (readFloat(buf) * 1000);
            if (weekNumber == 0) {
                weekNumber = buf.readUnsignedShort();
            }

            DateBuilder dateBuilder = new DateBuilder().setDate(1980, 1, 6);
            dateBuilder.addMillis(weekNumber * 7 * 24 * 60 * 60 * 1000 + weekTime);

            return dateBuilder.getDate();
        }

    }

    private List<Position> parseFormatA(DeviceSession deviceSession, ByteBuf buf) {
        List<Position> positions = new LinkedList<>();

        FloatReader latitudeReader = new FloatReader();
        FloatReader longitudeReader = new FloatReader();
        TimeReader timeReader = new TimeReader();

        try {
            while (buf.isReadable()) {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                short flags = buf.readUnsignedByte();

                short event = buf.readUnsignedByte();
                if (BitUtil.check(event, 7)) {
                    if (BitUtil.check(event, 6)) {
                        buf.skipBytes(8);
                    } else {
                        while (BitUtil.check(event, 7)) {
                            event = buf.readUnsignedByte();
                        }
                    }
                }

                position.setLatitude(latitudeReader.readFloat(buf) / Math.PI * 180);
                position.setLongitude(longitudeReader.readFloat(buf) / Math.PI * 180);
                position.setTime(timeReader.readTime(buf));

                if (BitUtil.check(flags, 0)) {
                    buf.readUnsignedByte(); // status
                }

                if (BitUtil.check(flags, 1)) {
                    position.setAltitude(buf.readUnsignedShort());
                }

                if (BitUtil.check(flags, 2)) {
                    position.setSpeed(buf.readUnsignedShort() & 0x03ff);
                    position.setCourse(buf.readUnsignedByte());
                }

                if (BitUtil.check(flags, 3)) {
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedShort() * 1000);
                }

                if (BitUtil.check(flags, 4)) {
                    position.set(Position.KEY_FUEL_CONSUMPTION + "Accumulator1", buf.readUnsignedInt());
                    position.set(Position.KEY_FUEL_CONSUMPTION + "Accumulator2", buf.readUnsignedInt());
                    position.set("hours1", buf.readUnsignedShort());
                    position.set("hours2", buf.readUnsignedShort());
                }

                if (BitUtil.check(flags, 5)) {
                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() & 0x03ff);
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort() & 0x03ff);
                    position.set(Position.PREFIX_ADC + 3, buf.readUnsignedShort() & 0x03ff);
                    position.set(Position.PREFIX_ADC + 4, buf.readUnsignedShort() & 0x03ff);
                }

                if (BitUtil.check(flags, 6)) {
                    position.set(Position.PREFIX_TEMP + 1, buf.readByte());
                    buf.getUnsignedByte(buf.readerIndex()); // control (>> 4)
                    position.set(Position.KEY_INPUT, buf.readUnsignedShort() & 0x0fff);
                    buf.readUnsignedShort(); // old sensor state (& 0x0fff)
                }

                if (BitUtil.check(flags, 7)) {
                    position.set(Position.KEY_BATTERY, buf.getUnsignedByte(buf.readerIndex()) >> 2);
                    position.set(Position.KEY_POWER, buf.readUnsignedShort() & 0x03ff);
                    position.set(Position.KEY_DEVICE_TEMP, buf.readByte());

                    position.set(Position.KEY_RSSI, (buf.getUnsignedByte(buf.readerIndex()) >> 4) & 0x07);

                    int satellites = buf.readUnsignedByte() & 0x0f;
                    position.setValid(satellites >= 3);
                    position.set(Position.KEY_SATELLITES, satellites);
                }
                positions.add(position);
            }
        } catch (IndexOutOfBoundsException error) {
            LOGGER.warn("MTA6 parsing error", error);
        }

        return positions;
    }

    private Position parseFormatA1(DeviceSession deviceSession, ByteBuf buf) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        short flags = buf.readUnsignedByte();

        // Skip events
        short event = buf.readUnsignedByte();
        if (BitUtil.check(event, 7)) {
            if (BitUtil.check(event, 6)) {
                buf.skipBytes(8);
            } else {
                while (BitUtil.check(event, 7)) {
                    event = buf.readUnsignedByte();
                }
            }
        }

        position.setLatitude(new FloatReader().readFloat(buf) / Math.PI * 180);
        position.setLongitude(new FloatReader().readFloat(buf) / Math.PI * 180);
        position.setTime(new TimeReader().readTime(buf));

        position.set(Position.KEY_STATUS, buf.readUnsignedByte());

        if (BitUtil.check(flags, 0)) {
            position.setAltitude(buf.readUnsignedShort());
            position.setSpeed(buf.readUnsignedByte());
            position.setCourse(buf.readByte());
            position.set(Position.KEY_ODOMETER, new FloatReader().readFloat(buf));
        }

        if (BitUtil.check(flags, 1)) {
            position.set(Position.KEY_FUEL_CONSUMPTION, new FloatReader().readFloat(buf));
            position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(new FloatReader().readFloat(buf)));
            position.set("tank", buf.readUnsignedByte() * 0.4);
        }

        if (BitUtil.check(flags, 2)) {
            position.set("engine", buf.readUnsignedShort() * 0.125);
            position.set("pedals", buf.readUnsignedByte());
            position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedByte() - 40);
            position.set(Position.KEY_ODOMETER_SERVICE, buf.readUnsignedShort());
        }

        if (BitUtil.check(flags, 3)) {
            position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 3, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 4, buf.readUnsignedShort());
        }

        if (BitUtil.check(flags, 4)) {
            position.set(Position.PREFIX_TEMP + 1, buf.readByte());
            buf.getUnsignedByte(buf.readerIndex()); // control (>> 4)
            position.set(Position.KEY_INPUT, buf.readUnsignedShort() & 0x0fff);
            buf.readUnsignedShort(); // old sensor state (& 0x0fff)
        }

        if (BitUtil.check(flags, 5)) {
            position.set(Position.KEY_BATTERY, buf.getUnsignedByte(buf.readerIndex()) >> 2);
            position.set(Position.KEY_POWER, buf.readUnsignedShort() & 0x03ff);
            position.set(Position.KEY_DEVICE_TEMP, buf.readByte());

            position.set(Position.KEY_RSSI, buf.getUnsignedByte(buf.readerIndex()) >> 5);

            int satellites = buf.readUnsignedByte() & 0x1f;
            position.setValid(satellites >= 3);
            position.set(Position.KEY_SATELLITES, satellites);
        }

        // other data

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        ByteBuf buf = request.content();

        buf.skipBytes("id=".length());
        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '&');
        String uniqueId = buf.toString(buf.readerIndex(), index - buf.readerIndex(), StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
        if (deviceSession == null) {
            return null;
        }
        buf.skipBytes(uniqueId.length());
        buf.skipBytes("&bin=".length());

        short packetId = buf.readUnsignedByte();
        short offset = buf.readUnsignedByte(); // dataOffset
        short packetCount = buf.readUnsignedByte();
        buf.readUnsignedByte(); // reserved
        buf.readUnsignedByte(); // timezone
        buf.skipBytes(offset - 5);

        if (channel != null) {
            sendContinue(channel);
            sendResponse(channel, packetId, packetCount);
        }

        if (packetId == 0x31 || packetId == 0x32 || packetId == 0x36) {
            if (simple) {
                return parseFormatA1(deviceSession, buf);
            } else {
                return parseFormatA(deviceSession, buf);
            }
        } // else if (0x34 0x38 0x4F 0x59)

        return null;
    }

}
