/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class TzoneProtocolDecoder extends BaseProtocolDecoder {

    public TzoneProtocolDecoder(TzoneProtocol protocol) {
        super(protocol);
    }

    private String decodeAlarm(Short value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_SOS;
            case 0x10:
                return Position.ALARM_LOW_BATTERY;
            case 0x11:
                return Position.ALARM_OVERSPEED;
            case 0x14:
                return Position.ALARM_BREAKING;
            case 0x15:
                return Position.ALARM_ACCELETATION;
            case 0x30:
                return Position.ALARM_PARKING;
            case 0x42:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x43:
                return Position.ALARM_GEOFENCE_ENTER;
            default:
                return null;
        }
    }

    private void decodeCards(Position position, ChannelBuffer buf) {

        int index = 1;
        for (int i = 0; i < 4; i++) {

            int blockLength = buf.readUnsignedShort();
            int blockEnd = buf.readerIndex() + blockLength;

            if (blockLength > 0) {

                int count = buf.readUnsignedByte();
                for (int j = 0; j < count; j++) {

                    int length = buf.readUnsignedByte();

                    boolean odd = length % 2 != 0;
                    if (odd) {
                        length += 1;
                    }

                    String num = ChannelBuffers.hexDump(buf.readBytes(length / 2));

                    if (odd) {
                        num = num.substring(1);
                    }

                    position.set("card" + index, num);
                }
            }

            buf.readerIndex(blockEnd);
        }

    }

    private void decodePassengers(Position position, ChannelBuffer buf) {

        int blockLength = buf.readUnsignedShort();
        int blockEnd = buf.readerIndex() + blockLength;

        if (blockLength > 0) {

            position.set("passengersOn", buf.readUnsignedMedium());
            position.set("passengersOff", buf.readUnsignedMedium());

        }

        buf.readerIndex(blockEnd);

    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShort(); // length
        if (buf.readUnsignedShort() != 0x2424) {
            return null;
        }
        int hardware = buf.readUnsignedShort(); // model
        buf.readUnsignedInt(); // firmware

        String imei = ChannelBuffers.hexDump(buf.readBytes(8)).substring(1);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setDeviceTime(new DateBuilder()
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()).getDate());

        // GPS info

        int blockLength = buf.readUnsignedShort();
        int blockEnd = buf.readerIndex() + blockLength;

        if (blockLength < 22) {
            return null;
        }

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

        double lat;
        double lon;

        if (hardware == 0x10A || hardware == 0x10B) {
            lat = buf.readUnsignedInt() / 600000.0;
            lon = buf.readUnsignedInt() / 600000.0;
        } else {
            lat = buf.readUnsignedInt() / 100000.0 / 60.0;
            lon = buf.readUnsignedInt() / 100000.0 / 60.0;
        }

        position.setFixTime(new DateBuilder()
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()).getDate());

        position.setSpeed(buf.readUnsignedShort() * 0.01);

        position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium());

        int flags = buf.readUnsignedShort();
        position.setCourse(BitUtil.to(flags, 9));
        if (!BitUtil.check(flags, 10)) {
            lat = -lat;
        }
        position.setLatitude(lat);
        if (BitUtil.check(flags, 9)) {
            lon = -lon;
        }
        position.setLongitude(lon);
        position.setValid(BitUtil.check(flags, 11));

        buf.readerIndex(blockEnd);

        // LBS info

        blockLength = buf.readUnsignedShort();
        blockEnd = buf.readerIndex() + blockLength;

        if (blockLength > 0 && (hardware == 0x10A || hardware == 0x10B)) {
            position.setNetwork(new Network(
                    CellTower.fromLacCid(buf.readUnsignedShort(), buf.readUnsignedShort())));
        }

        buf.readerIndex(blockEnd);

        // Status info

        blockLength = buf.readUnsignedShort();
        blockEnd = buf.readerIndex() + blockLength;

        if (blockLength >= 13) {
            position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));
            buf.readUnsignedByte(); // terminal info
            position.set(Position.PREFIX_IO + 1, buf.readUnsignedShort());
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            buf.readUnsignedByte(); // GSM status
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort());
            position.set(Position.KEY_POWER, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
        }

        if (blockLength >= 15) {
            position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedShort());
        }

        buf.readerIndex(blockEnd);

        if (hardware == 0x10A || hardware == 0x10B) {

            decodeCards(position, buf);

            buf.skipBytes(buf.readUnsignedShort()); // temperature
            buf.skipBytes(buf.readUnsignedShort()); // lock

            decodePassengers(position, buf);

        }

        return position;
    }

}
