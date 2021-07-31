/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class FlexibleReportProtocolDecoder extends BaseProtocolDecoder {

    public FlexibleReportProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_GENERAL = 0x00;

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int index) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x7E); // header
            response.writeShort(2); // length
            response.writeByte(0xE0);
            response.writeByte(BitUtil.check(index, 0) ? 0x4F : 0x0F);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private Date decodeTime(ByteBuf buf) {
        int timestamp = buf.readInt();
        return new DateBuilder()
                .setSecond(timestamp % 60)
                .setMinute((timestamp / 60) % 60)
                .setHour((timestamp / (60 * 60)) % 24)
                .setDay(1 + timestamp / (60 * 60 * 24) % 31)
                .setMonth(1 + timestamp / (60 * 60 * 24 * 31) % 12)
                .setYear(2000 + timestamp / (60 * 60 * 24 * 31 * 12))
                .getDate();
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        int flags = buf.readUnsignedByte();

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        int index = buf.readUnsignedShort();

        if (BitUtil.to(flags, 2) > 0) {
            sendResponse(channel, remoteAddress, index);
        }

        Date time = decodeTime(buf);
        int event = buf.readUnsignedByte();

        buf.readUnsignedByte(); // length

        int type = buf.readUnsignedByte();

        if (type == MSG_GENERAL) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setDeviceTime(time);

            position.set(Position.KEY_EVENT, event);

            buf.readUnsignedByte(); // length
            long mask = buf.readUnsignedInt();

            if (BitUtil.check(mask, 0)) {
                buf.readUnsignedByte(); // product id
            }
            if (BitUtil.check(mask, 1)) {
                position.setFixTime(decodeTime(buf));
            }
            if (BitUtil.check(mask, 2)) {
                position.setValid(true);
                position.setLatitude(buf.readUnsignedInt() / 1000000.0 - 90);
                position.setLongitude(buf.readUnsignedInt() / 1000000.0 - 180);
            }
            if (BitUtil.check(mask, 3)) {
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
                position.setCourse(buf.readUnsignedShort());
            }
            if (BitUtil.check(mask, 4)) {
                position.setAltitude(buf.readShort());
            }
            if (BitUtil.check(mask, 5)) {
                buf.readUnsignedShort(); // gps accuracy
            }
            if (BitUtil.check(mask, 6)) {
                position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
            }
            if (BitUtil.check(mask, 7)) {
                position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
            }
            if (BitUtil.check(mask, 8)) {
                position.set("auxPower", buf.readUnsignedShort() * 0.001);
            }
            if (BitUtil.check(mask, 9)) {
                position.set("solarPower", buf.readUnsignedShort() * 0.001);
            }
            if (BitUtil.check(mask, 10)) {
                int cellService = buf.readUnsignedByte();
                position.set(Position.KEY_ROAMING, BitUtil.check(cellService, 7));
                position.set("service", BitUtil.to(cellService, 7));
                buf.skipBytes(4); // cell info
            }
            if (BitUtil.check(mask, 11)) {
                buf.readUnsignedByte(); // rssi
            }
            if (BitUtil.check(mask, 12)) {
                int inputs = buf.readUnsignedByte();
                position.set(Position.KEY_IGNITION, BitUtil.check(inputs, 0));
                position.set(Position.PREFIX_IO + 1, inputs);
            }
            if (BitUtil.check(mask, 13)) {
                position.set(Position.PREFIX_IO + 2, buf.readUnsignedByte());
            }
            if (BitUtil.check(mask, 14)) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);
            }
            if (BitUtil.check(mask, 15)) {
                position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedShort() * 0.01);
            }

            return position;

        }

        return null;
    }

}
