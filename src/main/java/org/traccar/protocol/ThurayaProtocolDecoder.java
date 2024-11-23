/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class ThurayaProtocolDecoder extends BaseProtocolDecoder {

    public ThurayaProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_EVENT = 0x5101;
    public static final int MSG_PERIODIC_REPORT = 0x7101;
    public static final int MSG_SETTING_RESPONSE = 0x8115;
    public static final int MSG_ACK = 0x9901;

    private static int checksum(ByteBuffer buf) {
        int crc = 0;
        while (buf.hasRemaining()) {
            crc += buf.get();
        }
        crc = ~crc;
        crc += 1;
        return crc;
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, long id, int type) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeCharSequence("#T", StandardCharsets.US_ASCII);
            response.writeShort(15); // length
            response.writeShort(MSG_ACK);
            response.writeInt((int) id);
            response.writeShort(type);
            response.writeShort(1); // server ok
            response.writeShort(checksum(response.nioBuffer()));
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private void decodeLocation(ByteBuf buf, Position position) {

        position.setValid(true);

        DateBuilder dateBuilder = new DateBuilder();

        int date = buf.readInt();
        dateBuilder.setDay(date % 100);
        date /= 100;
        dateBuilder.setMonth(date % 100);
        date /= 100;
        dateBuilder.setYear(date);

        int time = buf.readInt();
        dateBuilder.setSecond(time % 100);
        time /= 100;
        dateBuilder.setMinute(time % 100);
        time /= 100;
        dateBuilder.setHour(time);

        position.setTime(dateBuilder.getDate());

        position.setLongitude(buf.readInt() / 1000000.0);
        position.setLatitude(buf.readInt() / 1000000.0);

        int data = buf.readUnsignedShort();

        int ignition = BitUtil.from(data, 12);
        if (ignition == 1) {
            position.set(Position.KEY_IGNITION, true);
        } else if (ignition == 2) {
            position.set(Position.KEY_IGNITION, false);
        }

        position.setCourse(BitUtil.to(data, 12));
        position.setSpeed(buf.readShort());

        position.set(Position.KEY_RPM, buf.readShort());

        position.set("data", readString(buf));
    }

    private String decodeAlarm(int event) {
        return switch (event) {
            case 10 -> Position.ALARM_VIBRATION;
            case 11 -> Position.ALARM_OVERSPEED;
            case 12 -> Position.ALARM_POWER_CUT;
            case 13 -> Position.ALARM_LOW_BATTERY;
            case 18 -> Position.ALARM_GPS_ANTENNA_CUT;
            case 20 -> Position.ALARM_ACCELERATION;
            case 21 -> Position.ALARM_BRAKING;
            default -> null;
        };
    }

    private String readString(ByteBuf buf) {
        int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0);
        CharSequence value = buf.readCharSequence(endIndex - buf.readerIndex(), StandardCharsets.US_ASCII);
        buf.readUnsignedByte(); // delimiter
        return value.toString();
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // service
        buf.readUnsignedShort(); // length
        int type = buf.readUnsignedShort();
        long id = buf.readUnsignedInt();

        sendResponse(channel, remoteAddress, id, type);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
        if (deviceSession == null) {
            return null;
        }

        if (type == MSG_EVENT) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            decodeLocation(buf, position);

            int event = buf.readUnsignedByte();
            position.addAlarm(decodeAlarm(event));
            position.set(Position.KEY_EVENT, event);
            position.set("eventData", readString(buf));

            return position;

        } else if (type == MSG_PERIODIC_REPORT) {

            List<Position> positions = new LinkedList<>();

            int count = buf.readUnsignedByte();
            for (int i = 0; i < count; i++) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                decodeLocation(buf, position);

                positions.add(position);

            }

            return positions;

        }

        return null;
    }

}
