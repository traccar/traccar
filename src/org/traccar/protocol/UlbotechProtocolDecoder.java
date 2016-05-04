/*
 * Copyright 2015 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.Context;
import org.traccar.helper.BitUtil;
import org.traccar.helper.ObdDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class UlbotechProtocolDecoder extends BaseProtocolDecoder {

    private final long timeZone;

    public UlbotechProtocolDecoder(UlbotechProtocol protocol) {
        super(protocol);
        timeZone = Context.getConfig().getInteger(getProtocolName() + ".timezone", 0);
    }

    private static final short DATA_GPS = 0x01;
    private static final short DATA_LBS = 0x02;
    private static final short DATA_STATUS = 0x03;
    private static final short DATA_ODOMETER = 0x04;
    private static final short DATA_ADC = 0x05;
    private static final short DATA_GEOFENCE = 0x06;
    private static final short DATA_OBD2 = 0x07;
    private static final short DATA_FUEL = 0x08;
    private static final short DATA_OBD2_ALARM = 0x09;
    private static final short DATA_HARSH_DRIVER = 0x0A;
    private static final short DATA_CANBUS = 0x0B;
    private static final short DATA_J1708 = 0x0C;
    private static final short DATA_VIN = 0x0D;
    private static final short DATA_RFID = 0x0E;
    private static final short DATA_EVENT = 0x10;

    private void decodeObd(Position position, ChannelBuffer buf, short length) {

        int end = buf.readerIndex() + length;

        while (buf.readerIndex() < end) {
            int parameterLength = buf.getUnsignedByte(buf.readerIndex()) >> 4;
            int mode = buf.readUnsignedByte() & 0x0F;
            position.add(ObdDecoder.decode(mode, ChannelBuffers.hexDump(buf.readBytes(parameterLength - 1))));
        }
    }

    private void decodeDriverBehavior(Position position, ChannelBuffer buf) {

        int value = buf.readUnsignedByte();

        if (BitUtil.check(value, 0)) {
            position.set("rapid-acceleration", true);
        }
        if (BitUtil.check(value, 1)) {
            position.set("rough-braking", true);
        }
        if (BitUtil.check(value, 2)) {
            position.set("harsh-course", true);
        }
        if (BitUtil.check(value, 3)) {
            position.set("no-warm-up", true);
        }
        if (BitUtil.check(value, 4)) {
            position.set("long-idle", true);
        }
        if (BitUtil.check(value, 5)) {
            position.set("fatigue-driving", true);
        }
        if (BitUtil.check(value, 6)) {
            position.set("rough-terrain", true);
        }
        if (BitUtil.check(value, 7)) {
            position.set("high-rpm", true);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (buf.readUnsignedByte() != 0xF8) {
            return null;
        }

        buf.readUnsignedByte(); // version
        buf.readUnsignedByte(); // type

        Position position = new Position();
        position.setProtocol(getProtocolName());

        String imei = ChannelBuffers.hexDump(buf.readBytes(8)).substring(1);
        if (!identify(imei, channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        long seconds = buf.readUnsignedInt() & 0x7fffffffL;
        seconds += 946684800L; // 2000-01-01 00:00
        seconds -= timeZone;
        Date time = new Date(seconds * 1000);

        boolean hasLocation = false;

        while (buf.readableBytes() > 3) {

            short type = buf.readUnsignedByte();
            short length = buf.readUnsignedByte();

            switch (type) {

                case DATA_GPS:
                    hasLocation = true;
                    position.setValid(true);
                    position.setLatitude(buf.readInt() / 1000000.0);
                    position.setLongitude(buf.readInt() / 1000000.0);
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
                    position.setCourse(buf.readUnsignedShort());
                    position.set(Event.KEY_HDOP, buf.readUnsignedShort());
                    break;

                case DATA_LBS:
                    position.set(Event.KEY_MCC, buf.readUnsignedShort());
                    position.set(Event.KEY_MNC, buf.readUnsignedShort());
                    position.set(Event.KEY_LAC, buf.readUnsignedShort());
                    if (length == 11) {
                        position.set(Event.KEY_CID, buf.readUnsignedInt());
                    } else {
                        position.set(Event.KEY_CID, buf.readUnsignedShort());
                    }
                    position.set(Event.KEY_GSM, -buf.readUnsignedByte());
                    if (length > 9 && length != 11) {
                        buf.skipBytes(length - 9);
                    }
                    break;

                case DATA_STATUS:
                    int status = buf.readUnsignedShort();
                    position.set(Event.KEY_IGNITION, BitUtil.check(status, 9));
                    position.set(Event.KEY_STATUS, status);
                    position.set(Event.KEY_ALARM, buf.readUnsignedShort());
                    break;

                case DATA_ODOMETER:
                    position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());
                    break;

                case DATA_ADC:
                    for (int i = 0; i < length / 2; i++) {
                        int value = buf.readUnsignedShort();
                        position.set(Event.PREFIX_ADC + BitUtil.from(value, 12), BitUtil.to(value, 12));
                    }
                    break;

                case DATA_GEOFENCE:
                    position.set("geofence-in", buf.readUnsignedInt());
                    position.set("geofence-alarm", buf.readUnsignedInt());
                    break;

                case DATA_OBD2:
                    decodeObd(position, buf, length);
                    break;

                case DATA_FUEL:
                    position.set("fuel-consumption", buf.readUnsignedInt() / 10000.0);
                    break;

                case DATA_OBD2_ALARM:
                    decodeObd(position, buf, length);
                    break;

                case DATA_HARSH_DRIVER:
                    decodeDriverBehavior(position, buf);
                    break;

                case DATA_CANBUS:
                    position.set("can", ChannelBuffers.hexDump(buf.readBytes(length)));
                    break;

                case DATA_J1708:
                    position.set("j1708", ChannelBuffers.hexDump(buf.readBytes(length)));
                    break;

                case DATA_VIN:
                    position.set(Event.KEY_VIN, buf.readBytes(length).toString(StandardCharsets.US_ASCII));
                    break;

                case DATA_RFID:
                    position.set(Event.KEY_RFID, buf.readBytes(length - 1).toString(StandardCharsets.US_ASCII));
                    position.set("authorized", buf.readUnsignedByte() != 0);
                    break;

                case DATA_EVENT:
                    position.set(Event.KEY_EVENT, buf.readUnsignedByte());
                    if (length > 1) {
                        position.set("event-mask", buf.readUnsignedInt());
                    }
                    break;

                default:
                    buf.skipBytes(length);
                    break;
            }
        }

        if (!hasLocation) {
            getLastLocation(position, time);
        } else {
            position.setTime(time);
        }

        return position;
    }

}
