/*
 * Copyright 2020 - 2023 Anton Tananaev (anton@traccar.org)
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
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.traccar.BaseMqttProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class IotmProtocolDecoder extends BaseMqttProtocolDecoder {

    public IotmProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Object readValue(ByteBuf buf, int sensorType) {
        switch (sensorType) {
            case 0:
                return false;
            case 1:
                return true;
            case 3:
                return 0;
            case 4:
                return buf.readUnsignedByte();
            case 5:
                return buf.readUnsignedShortLE();
            case 6:
                return buf.readUnsignedIntLE();
            case 7:
            case 11:
                return buf.readLongLE();
            case 8:
                return buf.readByte();
            case 9:
                return buf.readShortLE();
            case 10:
                return buf.readIntLE();
            case 12:
                return buf.readFloatLE();
            case 13:
                return buf.readDoubleLE();
            case 32:
                return buf.readCharSequence(buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString();
            case 33:
                return ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedByte()));
            case 64:
                return buf.readCharSequence(buf.readUnsignedShortLE(), StandardCharsets.US_ASCII).toString();
            case 65:
                return ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedShortLE()));
            case 2:
            default:
                return null;
        }
    }

    private void decodeSensor(Position position, ByteBuf record, int sensorType, int sensorId) {
        String key;
        switch (sensorId) {
            case 0x0002:
                position.set(Position.KEY_MOTION, sensorType > 0);
                break;
            case 0x0008:
            case 0x009B:
                if (sensorType > 0) {
                    position.set(Position.KEY_ALARM, Position.ALARM_JAMMING);
                }
                break;
            case 0x0010:
            case 0x0011:
            case 0x0012:
            case 0x0013:
            case 0x0014:
            case 0x0015:
                key = Position.PREFIX_IN + (sensorId - 0x0010 + 2);
                position.set(key, sensorType > 0);
                break;
            case 0x0062:
                position.set("doorFL", sensorType > 0);
                break;
            case 0x0063:
                position.set("doorFR", sensorType > 0);
                break;
            case 0x0064:
                position.set("doorRL", sensorType > 0);
                break;
            case 0x0065:
                position.set("doorRR", sensorType > 0);
                break;
            case 0x001E:
                position.set("buttonPresent", sensorType > 0);
                break;
            case 0x006D:
                position.set(Position.KEY_IGNITION, sensorType > 0);
                break;
            case 0x008B:
                position.set("handBrake", sensorType > 0);
                break;
            case 0x008C:
                position.set("footBrake", sensorType > 0);
                break;
            case 0x0094:
            case 0x0095:
            case 0x0096:
                key = Position.PREFIX_OUT + (sensorId - 0x0094 + 1);
                position.set(key, sensorType > 0);
                break;
            case 0x009A:
                position.set(Position.PREFIX_OUT + 4, sensorType > 0);
                break;
            case 0x2000:
                position.set(Position.KEY_OBD_SPEED, record.readUnsignedByte());
                break;
            case 0x2001:
                position.set(Position.KEY_SATELLITES, record.readUnsignedByte());
                break;
            case 0x2006:
                position.set(Position.KEY_THROTTLE, record.readUnsignedByte());
                break;
            case 0x2007:
                position.set(Position.KEY_FUEL_LEVEL, record.readUnsignedByte());
                break;
            case 0x2008:
                position.set(Position.KEY_COOLANT_TEMP, record.readUnsignedByte());
                break;
            case 0x2009:
                position.set("fuel2", record.readUnsignedByte());
                break;
            case 0x200A:
                position.set(Position.KEY_ENGINE_LOAD, record.readUnsignedByte());
                break;
            case 0x2041:
                position.set(Position.KEY_BATTERY_LEVEL, record.readUnsignedByte());
                break;
            case 0x3000:
                position.set(Position.KEY_POWER, record.readUnsignedShortLE() * 0.001);
                break;
            case 0x3001:
            case 0x3002:
            case 0x3003:
                key = Position.PREFIX_ADC + (0x3003 - sensorId + 3);
                position.set(key, record.readUnsignedShortLE() * 0.001);
                break;
            case 0x3004:
                position.set(Position.KEY_BATTERY, record.readUnsignedShortLE() * 0.001);
                break;
            case 0x300C:
                position.set(Position.KEY_RPM, record.readUnsignedShortLE());
                break;
            case 0x3021:
                position.set(Position.KEY_FUEL_CONSUMPTION, record.readUnsignedShortLE() * 0.05);
                break;
            case 0x3037:
                position.set("cargoWeight", record.readUnsignedShortLE() * 2);
                break;
            case 0x4001:
                position.set(Position.KEY_FUEL_USED, record.readUnsignedIntLE());
                break;
            case 0x4002:
                position.set(Position.KEY_HOURS, record.readUnsignedIntLE());
                break;
            case 0x4003:
                position.set(Position.KEY_ODOMETER, record.readUnsignedIntLE() * 5);
                break;
            case 0x4063:
                position.set(Position.KEY_AXLE_WEIGHT, record.readUnsignedIntLE());
                break;
            case 0x5000:
                position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(record.readLongLE()));
                break;
            case 0x5004:
            case 0x5005:
            case 0x5006:
            case 0x5007:
                key = Position.PREFIX_TEMP + (sensorId - 0x5004 + 1);
                position.set(key, record.readLongLE());
                break;
            case 0x500D:
                position.set("trailerId", String.valueOf(record.readLongLE()));
                break;
            case 0xA000:
                position.set(Position.KEY_DEVICE_TEMP, record.readFloatLE());
                break;
            case 0xA001:
                position.set(Position.KEY_ACCELERATION, record.readFloatLE());
                break;
            case 0xA002:
                position.set("cornering", record.readFloatLE());
                break;
            case 0xA017:
            case 0xA018:
            case 0xA019:
            case 0xA01A:
                key = Position.PREFIX_TEMP + (sensorId - 0xA017 + 1);
                position.set(key, record.readFloatLE());
                break;
            case 0xB002:
                position.set(Position.KEY_OBD_ODOMETER, record.readDoubleLE());
                break;
            default:
                key = Position.PREFIX_IO + sensorId;
                position.getAttributes().put(key, readValue(record, sensorType));
                break;
        }
    }

    @Override
    protected Object decode(
            DeviceSession deviceSession, MqttPublishMessage message) throws Exception {

        List<Position> positions = new LinkedList<>();

        ByteBuf buf = message.payload();

        buf.readUnsignedByte(); // structure version

        while (buf.readableBytes() > 1) {
            int type = buf.readUnsignedByte();
            int length = buf.readUnsignedShortLE();
            ByteBuf record = buf.readSlice(length);
            if (type == 1) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());
                position.setTime(new Date(record.readUnsignedIntLE() * 1000));

                while (record.readableBytes() > 0) {
                    int sensorType = record.readUnsignedByte();
                    int sensorId = record.readUnsignedShortLE();
                    if (sensorType == 14) {

                        position.setValid(true);
                        position.setLatitude(record.readFloatLE());
                        position.setLongitude(record.readFloatLE());
                        position.setSpeed(UnitsConverter.knotsFromKph(record.readUnsignedShortLE()));

                        position.set(Position.KEY_HDOP, record.readUnsignedByte());
                        position.set(Position.KEY_SATELLITES, record.readUnsignedByte());

                        position.setCourse(record.readUnsignedShortLE());
                        position.setAltitude(record.readShortLE());

                    } else {

                        if (sensorType == 3) {
                            continue;
                        }

                        decodeSensor(position, record, sensorType, sensorId);

                    }
                }

                positions.add(position);

            } else if (type == 3) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                getLastLocation(position, new Date(record.readUnsignedIntLE() * 1000));

                record.readUnsignedByte(); // function identifier

                position.set(Position.KEY_EVENT, record.readUnsignedByte());

                positions.add(position);

            }
        }

        buf.readUnsignedByte(); // checksum

        return positions.isEmpty() ? null : positions;
    }

}
