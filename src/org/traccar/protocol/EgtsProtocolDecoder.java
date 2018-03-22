/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.BitUtil;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class EgtsProtocolDecoder extends BaseProtocolDecoder {

    public EgtsProtocolDecoder(EgtsProtocol protocol) {
        super(protocol);
    }

    public static final int SERVICE_AUTH = 1;
    public static final int SERVICE_TELEDATA = 2;
    public static final int SERVICE_COMMANDS = 4;
    public static final int SERVICE_FIRMWARE = 9;
    public static final int SERVICE_ECALL = 10;

    public static final int MSG_RECORD_RESPONSE = 0;
    public static final int MSG_TERM_IDENTITY = 1;
    public static final int MSG_MODULE_DATA = 2;
    public static final int MSG_VEHICLE_DATA = 3;
    public static final int MSG_AUTH_PARAMS = 4;
    public static final int MSG_AUTH_INFO = 5;
    public static final int MSG_SERVICE_INFO = 6;
    public static final int MSG_RESULT_CODE = 7;
    public static final int MSG_POS_DATA = 16;
    public static final int MSG_EXT_POS_DATA = 17;
    public static final int MSG_AD_SENSORS_DATA = 18;
    public static final int MSG_COUNTERS_DATA = 19;
    public static final int MSG_STATE_DATA = 20;
    public static final int MSG_LOOPIN_DATA = 22;
    public static final int MSG_ABS_DIG_SENS_DATA = 23;
    public static final int MSG_ABS_AN_SENS_DATA = 24;
    public static final int MSG_ABS_CNTR_DATA = 25;
    public static final int MSG_ABS_LOOPIN_DATA = 26;
    public static final int MSG_LIQUID_LEVEL_SENSOR = 27;
    public static final int MSG_PASSENGERS_COUNTERS  = 28;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(buf.getUnsignedByte(buf.readerIndex() + 3));

        DeviceSession deviceSession = null;
        List<Position> positions = new LinkedList<>();

        while (buf.readableBytes() > 2) {

            int length = buf.readUnsignedShort();

            buf.readUnsignedShort(); // index

            int recordFlags = buf.readUnsignedByte();

            if (BitUtil.check(recordFlags, 0)) {
                String deviceId = String.valueOf(buf.readUnsignedInt());
                if (deviceSession == null) {
                    deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
                }
            }

            if (deviceSession == null) {
                deviceSession = getDeviceSession(channel, remoteAddress);
            }

            if (BitUtil.check(recordFlags, 1)) {
                buf.readUnsignedInt(); // event id
            }
            if (BitUtil.check(recordFlags, 2)) {
                buf.readUnsignedInt(); // time
            }

            buf.readUnsignedByte(); // source service type
            buf.readUnsignedByte(); // recipient service type

            int recordEnd = buf.readerIndex() + length;

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            while (buf.readerIndex() < recordEnd) {
                int type = buf.readUnsignedByte();
                int end = buf.readUnsignedShort() + buf.readerIndex();

                if (type == MSG_POS_DATA) {

                    position.setTime(new Date((buf.readUnsignedInt() + 1262304000) * 1000)); // since 2010-01-01
                    position.setLatitude(buf.readUnsignedInt() * 90.0 / 0xFFFFFFFFL);
                    position.setLongitude(buf.readUnsignedInt() * 180.0 / 0xFFFFFFFFL);

                    int flags = buf.readUnsignedByte();
                    position.setValid(BitUtil.check(flags, 0));
                    if (BitUtil.check(flags, 5)) {
                        position.setLatitude(-position.getLatitude());
                    }
                    if (BitUtil.check(flags, 6)) {
                        position.setLongitude(-position.getLongitude());
                    }

                    int speed = buf.readUnsignedShort();
                    position.setSpeed(BitUtil.to(speed, 14));
                    position.setCourse(buf.readUnsignedByte() + (BitUtil.check(speed, 15) ? 0x100 : 0));

                    position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium() * 100);
                    position.set(Position.KEY_INPUT, buf.readUnsignedByte());
                    position.set(Position.KEY_EVENT, buf.readUnsignedByte());

                    if (BitUtil.check(flags, 7)) {
                        position.setAltitude(buf.readMedium());
                    }

                } else if (type == MSG_EXT_POS_DATA) {

                    int flags = buf.readUnsignedByte();

                    if (BitUtil.check(flags, 0)) {
                        position.set(Position.KEY_VDOP, buf.readUnsignedShort());
                    }
                    if (BitUtil.check(flags, 1)) {
                        position.set(Position.KEY_HDOP, buf.readUnsignedShort());
                    }
                    if (BitUtil.check(flags, 2)) {
                        position.set(Position.KEY_PDOP, buf.readUnsignedShort());
                    }
                    if (BitUtil.check(flags, 3)) {
                        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    }

                } else if (type == MSG_AD_SENSORS_DATA) {

                    buf.readUnsignedByte(); // inputs flags

                    position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());

                    buf.readUnsignedByte(); // adc flags

                }

                buf.readerIndex(end);
            }

            positions.add(position);
        }

        return positions.isEmpty() ? null : positions;
    }

}
