/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class NoranProtocolDecoder extends BaseProtocolDecoder {

    public NoranProtocolDecoder(NoranProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_UPLOAD_POSITION = 0x0008;
    public static final int MSG_UPLOAD_POSITION_NEW = 0x0032;
    public static final int MSG_CONTROL_RESPONSE = 0x8009;
    public static final int MSG_ALARM = 0x0003;
    public static final int MSG_SHAKE_HAND = 0x0000;
    public static final int MSG_SHAKE_HAND_RESPONSE = 0x8000;
    public static final int MSG_IMAGE_SIZE = 0x0200;
    public static final int MSG_IMAGE_PACKET = 0x0201;


    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedShort(); // length
        int type = buf.readUnsignedShort();

        if (type == MSG_SHAKE_HAND && channel != null) {

            ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 13);
            response.writeBytes(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "\r\n*KW", Charset.defaultCharset()));
            response.writeByte(0);
            response.writeShort(response.capacity());
            response.writeShort(MSG_SHAKE_HAND_RESPONSE);
            response.writeByte(1); // status
            response.writeBytes(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "\r\n", Charset.defaultCharset()));

            channel.write(response, remoteAddress);

        } else if (type == MSG_UPLOAD_POSITION ||
                 type == MSG_UPLOAD_POSITION_NEW ||
                 type == MSG_CONTROL_RESPONSE ||
                 type == MSG_ALARM) {

            boolean newFormat = false;
            if (type == MSG_UPLOAD_POSITION && buf.readableBytes() == 48 ||
                type == MSG_ALARM && buf.readableBytes() == 48 ||
                type == MSG_CONTROL_RESPONSE && buf.readableBytes() == 57 ||
                type == MSG_UPLOAD_POSITION_NEW) {
                newFormat = true;
            }

            // Create new position
            Position position = new Position();
            position.setProtocol(getProtocolName());

            if (type == MSG_CONTROL_RESPONSE) {
                buf.readUnsignedInt(); // GIS ip
                buf.readUnsignedInt(); // GIS port
            }

            // Flags
            int flags = buf.readUnsignedByte();
            position.setValid((flags & 0x01) != 0);

            // Alarm type
            position.set(Event.KEY_ALARM, buf.readUnsignedByte());

            // Location
            if (newFormat) {
                position.setSpeed(buf.readUnsignedInt());
                position.setCourse(buf.readFloat());
            } else {
                position.setSpeed(buf.readUnsignedByte());
                position.setCourse(buf.readUnsignedShort());
            }
            position.setLongitude(buf.readFloat());
            position.setLatitude(buf.readFloat());

            // Time
            if (!newFormat) {
                long timeValue = buf.readUnsignedInt();
                Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                time.clear();
                time.set(Calendar.YEAR, 2000 + (int) (timeValue >> 26));
                time.set(Calendar.MONTH, (int) (timeValue >> 22 & 0x0f) - 1);
                time.set(Calendar.DAY_OF_MONTH, (int) (timeValue >> 17 & 0x1f));
                time.set(Calendar.HOUR_OF_DAY, (int) (timeValue >> 12 & 0x1f));
                time.set(Calendar.MINUTE, (int) (timeValue >> 6 & 0x3f));
                time.set(Calendar.SECOND, (int) (timeValue & 0x3f));
                position.setTime(time.getTime());
            }

            // Identification
            String id = buf.readBytes(newFormat ? 12 : 11).toString(Charset.defaultCharset()).replaceAll("[^\\p{Print}]", "");
            if (!identify(id, channel, remoteAddress)) {
                return null;
            }
            position.setDeviceId(getDeviceId());

            // Time
            if (newFormat) {
                DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
                position.setTime(dateFormat.parse(buf.readBytes(17).toString(Charset.defaultCharset())));
                buf.readByte();
            }

            // Other data
            if (!newFormat) {
                position.set(Event.PREFIX_IO + 1, buf.readUnsignedByte());
                position.set(Event.KEY_FUEL, buf.readUnsignedByte());
            } else if (type == MSG_UPLOAD_POSITION_NEW) {
                position.set(Event.PREFIX_TEMP + 1, buf.readShort());
                position.set(Event.KEY_ODOMETER, buf.readFloat());
            }

            return position;
        }

        return null;
    }

}
