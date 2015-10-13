/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class ProgressProtocolDecoder extends BaseProtocolDecoder {

    private long lastIndex;
    private long newIndex;

    public ProgressProtocolDecoder(ProgressProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_NULL = 0;
    public static final int MSG_IDENT = 1;
    public static final int MSG_IDENT_FULL = 2;
    public static final int MSG_POINT = 10;
    public static final int MSG_LOG_SYNC = 100;
    public static final int MSG_LOGMSG = 101;
    public static final int MSG_TEXT = 102;
    public static final int MSG_ALARM = 200;
    public static final int MSG_ALARM_RECIEVED = 201;

    private static final String HEX_CHARS = "0123456789ABCDEF";

    private void requestArchive(Channel channel) {
        if (lastIndex == 0) {
            lastIndex = newIndex;
        } else if (newIndex > lastIndex) {
            ChannelBuffer request = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 12);
            request.writeShort(MSG_LOG_SYNC);
            request.writeShort(4);
            request.writeInt((int) lastIndex);
            request.writeInt(0);
            channel.write(request);
        }
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        int type = buf.readUnsignedShort();
        buf.readUnsignedShort(); // length

        if (type == MSG_IDENT || type == MSG_IDENT_FULL) {

            buf.readUnsignedInt(); // id
            int length = buf.readUnsignedShort();
            buf.skipBytes(length);
            length = buf.readUnsignedShort();
            buf.skipBytes(length);
            length = buf.readUnsignedShort();
            String imei = buf.readBytes(length).toString(Charset.defaultCharset());
            identify(imei, channel);

        } else if (hasDeviceId() && (type == MSG_POINT || type == MSG_ALARM || type == MSG_LOGMSG)) {

            List<Position> positions = new LinkedList<>();

            int recordCount = 1;
            if (type == MSG_LOGMSG) {
                recordCount = buf.readUnsignedShort();
            }

            for (int j = 0; j < recordCount; j++) {
                Position position = new Position();
                position.setProtocol(getProtocolName());
                position.setDeviceId(getDeviceId());

                // Message index
                if (type == MSG_LOGMSG) {
                    position.set(Event.KEY_ARCHIVE, true);
                    int subtype = buf.readUnsignedShort();
                    if (subtype == MSG_ALARM) {
                        position.set(Event.KEY_ALARM, true);
                    }
                    if (buf.readUnsignedShort() > buf.readableBytes()) {
                        lastIndex += 1;
                        break; // workaround for device bug
                    }
                    lastIndex = buf.readUnsignedInt();
                    position.set(Event.KEY_INDEX, lastIndex);
                } else {
                    newIndex = buf.readUnsignedInt();
                }

                // Time
                Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                time.clear();
                time.setTimeInMillis(buf.readUnsignedInt() * 1000);
                position.setTime(time.getTime());

                // Latitude
                position.setLatitude(buf.readInt() * 180.0 / 0x7FFFFFFF);

                // Longitude
                position.setLongitude(buf.readInt() * 180.0 / 0x7FFFFFFF);

                // Speed
                position.setSpeed(buf.readUnsignedInt() / 100.0);

                // Course
                position.setCourse(buf.readUnsignedShort() / 100.0);

                // Altitude
                position.setAltitude(buf.readUnsignedShort() / 100.0);

                // Satellites
                int satellitesNumber = buf.readUnsignedByte();
                position.set(Event.KEY_SATELLITES, satellitesNumber);

                // Validity
                position.setValid(satellitesNumber >= 3);

                // Cell signal
                position.set(Event.KEY_GSM, buf.readUnsignedByte());

                // Odometer
                position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());

                long extraFlags = buf.readLong();

                // Analog inputs
                if ((extraFlags & 0x1) == 0x1) {
                    int count = buf.readUnsignedShort();
                    for (int i = 1; i <= count; i++) {
                        position.set(Event.PREFIX_ADC + i, buf.readUnsignedShort());
                    }
                }

                // CAN adapter
                if ((extraFlags & 0x2) == 0x2) {
                    int size = buf.readUnsignedShort();
                    position.set("can", buf.toString(buf.readerIndex(), size, Charset.defaultCharset()));
                    buf.skipBytes(size);
                }

                // Passenger sensor
                if ((extraFlags & 0x4) == 0x4) {
                    int size = buf.readUnsignedShort();

                    // Convert binary data to hex
                    StringBuilder hex = new StringBuilder();
                    for (int i = buf.readerIndex(); i < buf.readerIndex() + size; i++) {
                        byte b = buf.getByte(i);
                        hex.append(HEX_CHARS.charAt((b & 0xf0) >> 4));
                        hex.append(HEX_CHARS.charAt(b & 0x0F));
                    }

                    position.set("passenger", hex.toString());

                    buf.skipBytes(size);
                }

                // Send response for alarm message
                if (type == MSG_ALARM) {
                    byte[] response = {(byte) 0xC9, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                    channel.write(ChannelBuffers.wrappedBuffer(response));

                    position.set(Event.KEY_ALARM, true);
                }

                // Skip CRC
                buf.readUnsignedInt();

                positions.add(position);
            }

            requestArchive(channel);

            return positions;
        }

        return null;
    }

}
