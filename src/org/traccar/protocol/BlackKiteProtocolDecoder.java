/*
 * Copyright 2015 Vijay Kumar (vijaykumar@zilogic.com)
 * Copyright 2013 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class BlackKiteProtocolDecoder extends BaseProtocolDecoder {

    public BlackKiteProtocolDecoder(BlackKiteProtocol protocol) {
        super(protocol);
    }

    private static final int TAG_IMEI = 0x03;
    private static final int TAG_DATE = 0x20;
    private static final int TAG_COORDINATES = 0x30;
    private static final int TAG_SPEED_COURSE = 0x33;
    private static final int TAG_ALTITUDE = 0x34;
    private static final int TAG_STATUS = 0x40;
    private static final int TAG_DIGITAL_OUTPUTS = 0x45;
    private static final int TAG_DIGITAL_INPUTS = 0x46;
    private static final int TAG_INPUT_VOLTAGE1 = 0x50;
    private static final int TAG_INPUT_VOLTAGE2 = 0x51;
    private static final int TAG_INPUT_VOLTAGE3 = 0x52;
    private static final int TAG_INPUT_VOLTAGE4 = 0x53;
    private static final int TAG_XT1 = 0x60;
    private static final int TAG_XT2 = 0x61;
    private static final int TAG_XT3 = 0x62;

    private void sendReply(Channel channel, int checksum) {
        ChannelBuffer reply = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 3);
        reply.writeByte(0x02);
        reply.writeShort((short) checksum);
        if (channel != null) {
            channel.write(reply);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedByte(); // header
        int length = (buf.readUnsignedShort() & 0x7fff) + 3;

        List<Position> positions = new LinkedList<>();
        Set<Integer> tags = new HashSet<>();
        boolean hasLocation = false;
        Position position = new Position();
        position.setProtocol(getProtocolName());

        while (buf.readerIndex() < length) {

            // Check if new message started
            int tag = buf.readUnsignedByte();
            if (tags.contains(tag)) {
                if (hasLocation && position.getFixTime() != null) {
                    positions.add(position);
                }
                tags.clear();
                hasLocation = false;
                position = new Position();
            }
            tags.add(tag);

            switch (tag) {

                case TAG_IMEI:
                    String imei = buf.toString(buf.readerIndex(), 15, Charset.defaultCharset());
                    buf.skipBytes(imei.length());
                    identify(imei, channel);
                    break;

                case TAG_DATE:
                    position.setTime(new Date(buf.readUnsignedInt() * 1000));
                    break;

                case TAG_COORDINATES:
                    hasLocation = true;
                    position.setValid((buf.readUnsignedByte() & 0xf0) == 0x00);
                    position.setLatitude(buf.readInt() / 1000000.0);
                    position.setLongitude(buf.readInt() / 1000000.0);
                    break;

                case TAG_SPEED_COURSE:
                    position.setSpeed(buf.readUnsignedShort() * 0.0539957);
                    position.setCourse(buf.readUnsignedShort() * 0.1);
                    break;

                case TAG_ALTITUDE:
                    position.setAltitude(buf.readShort());
                    break;

                case TAG_STATUS:
                    int status = buf.readUnsignedShort();
                    position.set(Event.KEY_IGNITION, BitUtil.check(status, 9));
                    position.set(Event.KEY_ALARM, BitUtil.check(status, 15));
                    position.set(Event.KEY_POWER, BitUtil.check(status, 2));
                    break;

                case TAG_DIGITAL_INPUTS:
                    int input = buf.readUnsignedShort();
                    for (int i = 0; i < 16; i++) {
                        position.set(Event.PREFIX_IO + (i + 1), BitUtil.check(input, i));
                    }
                    break;

                case TAG_DIGITAL_OUTPUTS:
                    int output = buf.readUnsignedShort();
                    for (int i = 0; i < 16; i++) {
                        position.set(Event.PREFIX_IO + (i + 17), BitUtil.check(output, i));
                    }
                    break;

                case TAG_INPUT_VOLTAGE1:
                    position.set(Event.PREFIX_ADC + 1, buf.readUnsignedShort() / 1000.0);
                    break;

                case TAG_INPUT_VOLTAGE2:
                    position.set(Event.PREFIX_ADC + 2, buf.readUnsignedShort() / 1000.0);
                    break;

                case TAG_INPUT_VOLTAGE3:
                    position.set(Event.PREFIX_ADC + 3, buf.readUnsignedShort() / 1000.0);
                    break;

                case TAG_INPUT_VOLTAGE4:
                    position.set(Event.PREFIX_ADC + 4, buf.readUnsignedShort() / 1000.0);
                    break;

                case TAG_XT1:
                case TAG_XT2:
                case TAG_XT3:
                    buf.skipBytes(16);
                    break;

                default:
                    break;

            }
        }

        if (hasLocation && position.getFixTime() != null) {
            positions.add(position);
        }

        if (!hasDeviceId()) {
            Log.warning("Unknown device");
            return null;
        }

        sendReply(channel, buf.readUnsignedShort());

        for (Position p : positions) {
            p.setDeviceId(getDeviceId());
        }

        if (positions.isEmpty()) {
            return null;
        }

        return positions;
    }

}
