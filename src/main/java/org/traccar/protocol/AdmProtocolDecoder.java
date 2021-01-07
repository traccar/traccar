/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AdmProtocolDecoder extends BaseProtocolDecoder {

    public AdmProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int CMD_RESPONSE_SIZE = 0x84;
    public static final int MSG_IMEI = 0x03;
    public static final int MSG_PHOTO = 0x0A;
    public static final int MSG_ADM5 = 0x01;

    private Position decodeData(Channel channel, SocketAddress remoteAddress, ByteBuf buf, int type) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (BitUtil.to(type, 2) == 0) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_VERSION_FW, buf.readUnsignedByte());
            position.set(Position.KEY_INDEX, buf.readUnsignedShortLE());

            int status = buf.readUnsignedShortLE();
            position.set(Position.KEY_STATUS, status);
            position.setValid(!BitUtil.check(status, 5));
            position.setLatitude(buf.readFloatLE());
            position.setLongitude(buf.readFloatLE());
            position.setCourse(buf.readUnsignedShortLE() * 0.1);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE() * 0.1));

            position.set(Position.KEY_ACCELERATION, buf.readUnsignedByte() * 0.1);
            position.setAltitude(buf.readShortLE());
            position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte() & 0x0f);

            position.setTime(new Date(buf.readUnsignedIntLE() * 1000));

            position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.001);
            position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);

            if (BitUtil.check(type, 2)) {
                buf.readUnsignedByte(); // vib
                buf.readUnsignedByte(); // vib_count

                int out = buf.readUnsignedByte();
                for (int i = 0; i <= 3; i++) {
                    position.set(Position.PREFIX_OUT + (i + 1), BitUtil.check(out, i) ? 1 : 0);
                }

                buf.readUnsignedByte(); // in_alarm
            }

            if (BitUtil.check(type, 3)) {
                for (int i = 1; i <= 6; i++) {
                    position.set(Position.PREFIX_ADC + i, buf.readUnsignedShortLE() * 0.001);
                }
            }

            if (BitUtil.check(type, 4)) {
                for (int i = 1; i <= 2; i++) {
                    position.set(Position.PREFIX_COUNT + i, buf.readUnsignedIntLE());
                }
            }

            if (BitUtil.check(type, 5)) {
                for (int i = 1; i <= 3; i++) {
                    buf.readUnsignedShortLE(); // fuel level
                }
                for (int i = 1; i <= 3; i++) {
                    position.set(Position.PREFIX_TEMP + i, buf.readUnsignedByte());
                }
            }

            if (BitUtil.check(type, 6)) {
                int endIndex = buf.readerIndex() + buf.readUnsignedByte();
                while (buf.readerIndex() < endIndex) {
                    int mask = buf.readUnsignedByte();
                    long value;
                    switch (BitUtil.from(mask, 6)) {
                        case 3:
                            value = buf.readLongLE();
                            break;
                        case 2:
                            value = buf.readUnsignedIntLE();
                            break;
                        case 1:
                            value = buf.readUnsignedShortLE();
                            break;
                        default:
                            value = buf.readUnsignedByte();
                            break;
                    }
                    int index = BitUtil.to(mask, 6);
                    switch (index) {
                        case 1:
                            position.set(Position.PREFIX_TEMP + 1, value);
                            break;
                        case 2:
                            position.set("humidity", value);
                            break;
                        case 3:
                            position.set("illumination", value);
                            break;
                        case 4:
                            position.set(Position.KEY_BATTERY, value);
                            break;
                        default:
                            position.set("can" + index, value);
                            break;
                    }
                }
            }

            if (BitUtil.check(type, 7)) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
            }

            return position;
        }

        return null;
    }

    private Position parseCommandResponse(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        int responseTextLength = buf.bytesBefore((byte) 0);
        if (responseTextLength < 0) {
            responseTextLength = CMD_RESPONSE_SIZE - 3;
        }
        position.set(Position.KEY_RESULT, buf.readSlice(responseTextLength).toString(StandardCharsets.UTF_8));

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (Character.isDigit(buf.getUnsignedByte(buf.readerIndex()))) {
            getDeviceSession(channel, remoteAddress, buf.readSlice(15).toString(StandardCharsets.UTF_8));
        }

        buf.readUnsignedShortLE(); // device id

        int size = buf.readUnsignedByte();
        if (size != CMD_RESPONSE_SIZE) {
            int type = buf.readUnsignedByte();
            if (type == MSG_IMEI) {
                getDeviceSession(channel, remoteAddress, buf.readSlice(15).toString(StandardCharsets.UTF_8));
            } else {
                return decodeData(channel, remoteAddress, buf, type);
            }
        } else {
            return parseCommandResponse(channel, remoteAddress, buf);
        }

        return null;
    }

}
