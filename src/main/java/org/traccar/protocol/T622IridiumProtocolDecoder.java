/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class T622IridiumProtocolDecoder extends BaseProtocolDecoder {

    private String format;

    public T622IridiumProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public List<Integer> getParameters(long deviceId) {
        String value = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_FORMAT.withPrefix(getProtocolName()), deviceId);
        return Arrays.stream((value != null ? value : format).split(","))
                .map(s -> Integer.parseInt(s, 16))
                .collect(Collectors.toList());
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // protocol revision
        buf.readUnsignedShort(); // length
        buf.readUnsignedByte(); // header indicator
        buf.readUnsignedShort(); // header length
        buf.readUnsignedInt(); // reference

        String imei = buf.readCharSequence(15, StandardCharsets.US_ASCII).toString();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        buf.readUnsignedByte(); // session status
        buf.readUnsignedShort(); // originator index
        buf.readUnsignedShort(); // transfer index
        buf.readUnsignedInt(); // session time
        buf.readUnsignedByte(); // payload indicator
        buf.readUnsignedShort(); // payload length

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        List<Integer> parameters = getParameters(deviceSession.getDeviceId());

        for (int parameter : parameters) {
            switch (parameter) {
                case 0x01:
                    position.set(Position.KEY_EVENT, buf.readUnsignedByte());
                    break;
                case 0x02:
                    position.setLatitude(buf.readIntLE() / 1000000.0);
                    break;
                case 0x03:
                    position.setLongitude(buf.readIntLE() / 1000000.0);
                    break;
                case 0x04:
                    position.setTime(new Date((buf.readUnsignedIntLE() + 946684800) * 1000));
                    break;
                case 0x05:
                    position.setValid(buf.readUnsignedByte() > 0);
                    break;
                case 0x06:
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    break;
                case 0x07:
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    break;
                case 0x08:
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
                    break;
                case 0x09:
                    position.setCourse(buf.readUnsignedShortLE());
                    break;
                case 0x0A:
                    position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
                    break;
                case 0x0B:
                    position.setAltitude(buf.readShortLE());
                    break;
                case 0x0C:
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
                    break;
                case 0x0D:
                    position.set(Position.KEY_HOURS, buf.readUnsignedIntLE() * 1000);
                    break;
                case 0x14:
                    position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
                    break;
                case 0x15:
                    position.set(Position.KEY_INPUT, buf.readUnsignedByte());
                    break;
                case 0x19:
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.01);
                    break;
                case 0x1A:
                    position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.01);
                    break;
                case 0x1B:
                    buf.readUnsignedByte(); // geofence
                    break;
                default:
                    break;
            }
        }

        return position;
    }

}
