/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.AdvancedConnection;
import org.traccar.helper.Log;
import org.traccar.helper.NamedParameterStatement;
import org.traccar.model.Position;

/**
 * Progress tracker protocol decoder
 */
public class ProgressProtocolDecoder extends BaseProtocolDecoder {

    private long deviceId;
    private long lastIndex;
    private long newIndex;

    public ProgressProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    /*
     * Message types
     */
    private static final int MSG_NULL = 0;
    private static final int MSG_IDENT = 1;
    private static final int MSG_IDENT_FULL = 2;
    private static final int MSG_POINT = 10;
    private static final int MSG_LOG_SYNC = 100;
    private static final int MSG_LOGMSG = 101;
    private static final int MSG_TEXT = 102;
    private static final int MSG_ALARM = 200;
    private static final int MSG_ALARM_RECIEVED = 201;

    private static final String HEX_CHARS = "0123456789ABCDEF";

    /**
     * Hack to load last index from database
     */
    /*private void loadLastIndex() {
        try {
            Properties p = getServerManager().getProperties();
            AdvancedConnection connection = new AdvancedConnection(
                    p.getProperty("database.url"), p.getProperty("database.user"), p.getProperty("database.password"));
            NamedParameterStatement queryLastIndex = new NamedParameterStatement(connection, p.getProperty("database.selectLastIndex"));
            queryLastIndex.prepare();
            queryLastIndex.setLong("device_id", deviceId);
            ResultSet result = queryLastIndex.executeQuery();
            if (result.next()) {
                lastIndex = result.getLong(1);
            }
        } catch(Exception error) {
        }
    }*/

    /**
     * Request archive messages
     */
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

    /**
     * Decode message
     */
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        int type = buf.readUnsignedShort();
        buf.readUnsignedShort(); // length

        // Authentication
        if (type == MSG_IDENT || type == MSG_IDENT_FULL) {
            long id = buf.readUnsignedInt();
            int length = buf.readUnsignedShort();
            buf.skipBytes(length);
            length = buf.readUnsignedShort();
            buf.skipBytes(length);
            length = buf.readUnsignedShort();
            String imei = buf.readBytes(length).toString(Charset.defaultCharset());
            try {
                deviceId = getDataManager().getDeviceByImei(imei).getId();
                //loadLastIndex();
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei + " (id - " + id + ")");
            }
        }

        // Position
        else if (/*deviceId != 0 &&*/ (type == MSG_POINT || type == MSG_ALARM || type == MSG_LOGMSG)) {
            List<Position> positions = new LinkedList<Position>();

            int recordCount = 1;
            if (type == MSG_LOGMSG) {
                recordCount = buf.readUnsignedShort();
            }

            for (int j = 0; j < recordCount; j++) {
                Position position = new Position();
                StringBuilder extendedInfo = new StringBuilder("<protocol>progress</protocol>");
                position.setDeviceId(deviceId);

                // Message index
                if (type == MSG_LOGMSG) {
                    extendedInfo.append("<archive>true</archive>");
                    int subtype = buf.readUnsignedShort();
                    if (subtype == MSG_ALARM) {
                        extendedInfo.append("<alarm>true</alarm>");
                    }
                    buf.readUnsignedShort(); // length
                    lastIndex = buf.readUnsignedInt();
                    position.setId(lastIndex);
                } else {
                    newIndex = buf.readUnsignedInt();
                }

                // Time
                Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                time.clear();
                time.setTimeInMillis(buf.readUnsignedInt() * 1000);
                position.setTime(time.getTime());

                // Latitude
                position.setLatitude(((double) buf.readInt()) / 0x7FFFFFFF * 180.0);

                // Longitude
                position.setLongitude(((double) buf.readInt()) / 0x7FFFFFFF * 180.0);

                // Speed
                position.setSpeed(((double) buf.readUnsignedInt()) / 100);

                // Course
                position.setCourse(((double) buf.readUnsignedShort()) / 100);

                // Altitude
                position.setAltitude(((double) buf.readUnsignedShort()) / 100);

                // Satellites
                int satellitesNumber = buf.readUnsignedByte();
                extendedInfo.append("<satellites>");
                extendedInfo.append(satellitesNumber);
                extendedInfo.append("</satellites>");

                // Validity
                position.setValid(satellitesNumber >= 3); // TODO: probably wrong

                // Cell signal
                extendedInfo.append("<gsm>");
                extendedInfo.append(buf.readUnsignedByte());
                extendedInfo.append("</gsm>");

                // Milage
                extendedInfo.append("<milage>");
                extendedInfo.append(buf.readUnsignedInt());
                extendedInfo.append("</milage>");

                long extraFlags = buf.readLong();

                // Analog inputs
                if ((extraFlags & 0x1) == 0x1) {
                    int count = buf.readUnsignedShort();
                    for (int i = 1; i <= count; i++) {
                        extendedInfo.append("<adc").append(i).append(">");
                        extendedInfo.append(buf.readUnsignedShort());
                        extendedInfo.append("</adc").append(i).append(">");
                    }

                }

                // CAN adapter
                if ((extraFlags & 0x2) == 0x2) {
                    int size = buf.readUnsignedShort();
                    extendedInfo.append("<can>");
                    extendedInfo.append(buf.toString(buf.readerIndex(), size, Charset.defaultCharset()));
                    extendedInfo.append("</can>");
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
                        hex.append(HEX_CHARS.charAt((b & 0x0F)));
                    }

                    extendedInfo.append("<passenger>");
                    extendedInfo.append(hex);
                    extendedInfo.append("</passenger>");

                    buf.skipBytes(size);
                }

                // Send response for alarm message
                if (type == MSG_ALARM) {
                    byte[] response = {(byte)0xC9,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
                    channel.write(ChannelBuffers.wrappedBuffer(response));

                    extendedInfo.append("<alarm>true</alarm>");
                }

                // Skip CRC
                buf.readUnsignedInt();

                // Extended info
                position.setExtendedInfo(extendedInfo.toString());

                positions.add(position);
            }

            requestArchive(channel);

            return positions;
        }

        return null;
    }

}
