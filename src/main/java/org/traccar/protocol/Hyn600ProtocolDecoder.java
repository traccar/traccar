/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class Hyn600ProtocolDecoder extends BaseProtocolDecoder {

    public Hyn600ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Position decodeReport(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedShort(); // protocol version
        buf.readUnsignedByte(); // event type
        buf.readUnsignedByte(); // event state
        long mask = buf.readUnsignedInt();

        if (BitUtil.check(mask, 0)) {
            buf.readUnsignedByte(); // frame count
            buf.readUnsignedByte(); // frame id
        }

        if (BitUtil.check(mask, 1)) {
            buf.readUnsignedByte(); // network type
        }

        if (BitUtil.check(mask, 2)) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 100.0);
            position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 3)) {
            position.setValid(buf.readUnsignedByte() > 0);
            position.set(Position.KEY_HDOP, buf.readUnsignedByte());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());
            position.setAltitude(buf.readShort());
            position.setLatitude(buf.readUnsignedInt() / 1000000.0);
            position.setLongitude(buf.readUnsignedInt() / 1000000.0);
            DateBuilder dateBuilder = new DateBuilder()
                    .setYear(BcdUtil.readInteger(buf, 4))
                    .setMonth(BcdUtil.readInteger(buf, 2))
                    .setDay(BcdUtil.readInteger(buf, 2))
                    .setHour(BcdUtil.readInteger(buf, 2))
                    .setMinute(BcdUtil.readInteger(buf, 2))
                    .setSecond(BcdUtil.readInteger(buf, 2));
            position.setFixTime(dateBuilder.getDate());
        } else {
            getLastLocation(position, null);
        }

        if (BitUtil.check(mask, 4)) {
            position.setNetwork(new Network(CellTower.from(
                    buf.readUnsignedShort(),
                    buf.readUnsignedShort(),
                    buf.readUnsignedShort(),
                    buf.readUnsignedInt(),
                    buf.readUnsignedByte())));
        }

        if (BitUtil.check(mask, 7)) {
            buf.readUnsignedByte(); // upgrade code
            buf.readUnsignedShort(); // reserved
        }

        if (BitUtil.check(mask, 8)) {
            long eventMask = buf.readUnsignedInt();
            if (BitUtil.check(eventMask, 0)) {
                position.set(Position.KEY_POWER, buf.readUnsignedShort() / 1000.0);
            }
            if (BitUtil.check(eventMask, 1)) {
                int adcIndex = buf.readUnsignedByte();
                position.set(Position.PREFIX_ADC + adcIndex, buf.readUnsignedShort() / 1000.0);
            }
            if (BitUtil.check(eventMask, 2)) {
                int status = buf.readUnsignedByte();
                position.set(Position.KEY_IGNITION, status >= 0x20);
                position.set(Position.KEY_MOTION, (status & 0xf) >= 0x2);
            }
            if (BitUtil.check(eventMask, 3)) {
                position.set(Position.KEY_INPUT, buf.readUnsignedByte());
            }
            if (BitUtil.check(eventMask, 4)) {
                position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
            }
            if (BitUtil.check(eventMask, 5)) {
                position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedShort() * 100);
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 100);
            }
            if (BitUtil.check(eventMask, 6)) {
                int satelliteMask = buf.readUnsignedByte();
                if (BitUtil.check(satelliteMask, 0)) {
                    buf.readUnsignedByte(); // gps satellites
                }
                if (BitUtil.check(satelliteMask, 1)) {
                    buf.readUnsignedByte(); // beidou satellites
                }
                if (BitUtil.check(satelliteMask, 2)) {
                    buf.readUnsignedByte(); // galileo satellites
                }
                if (BitUtil.check(satelliteMask, 3)) {
                    buf.readUnsignedByte(); // glonass satellites
                }
            }
            if (BitUtil.check(eventMask, 7)) {
                buf.readUnsignedInt(); // all geo status
            }
            if (BitUtil.check(eventMask, 8)) {
                buf.skipBytes(buf.readUnsignedByte()); // id data
            }
            if (BitUtil.check(eventMask, 9)) {
                int count = buf.readUnsignedByte();
                for (int i = 0; i < count; i++) {
                    buf.skipBytes(8); // sensor id
                    int dataMask = buf.readUnsignedByte();
                    if (BitUtil.check(dataMask, 0)) {
                        buf.readUnsignedShort(); // temperature
                    }
                    if (BitUtil.check(dataMask, 1)) {
                        buf.readUnsignedByte(); // humidity
                    }
                }
            }
            if (BitUtil.check(eventMask, 10)) {
                buf.skipBytes(3); // trip hours
                buf.skipBytes(5); // total hours
            }
            if (BitUtil.check(eventMask, 11)) {
                buf.skipBytes(9);
            }
            if (BitUtil.check(eventMask, 11)) {
                buf.skipBytes(8);
            }
        }

        if (BitUtil.check(mask, 16)) {
            long bleMask = buf.readUnsignedInt();
            if (BitUtil.check(bleMask, 0)) {
                buf.readUnsignedByte(); // ble relay number
                buf.readUnsignedByte(); // ble relay id
                buf.readUnsignedByte(); // ble relay alarm type
                int relayMask = buf.readUnsignedByte();
                if (BitUtil.check(relayMask, 0)) {
                    buf.skipBytes(6); // ble relay mac
                }
                if (BitUtil.check(relayMask, 1)) {
                    buf.readUnsignedShort(); // ble relay rssi
                }
                if (BitUtil.check(relayMask, 2)) {
                    buf.readUnsignedByte(); // reserved
                }
                if (BitUtil.check(relayMask, 3)) {
                    buf.readUnsignedByte(); // ble relay status
                }
                if (BitUtil.check(relayMask, 4)) {
                    buf.readUnsignedByte(); // ble relay error
                }
            }
            if (BitUtil.check(bleMask, 1)) {
                buf.readUnsignedShort(); // ble beacon number
                buf.readUnsignedByte(); // ble beacon model
                int beaconMask = buf.readUnsignedByte();
                if (BitUtil.check(beaconMask, 0)) {
                    buf.skipBytes(6); // beacon mac
                }
                if (BitUtil.check(beaconMask, 1)) {
                    buf.readUnsignedShort(); // beacon rssi
                }
                if (BitUtil.check(beaconMask, 2)) {
                    buf.readUnsignedShort(); // beacon battery
                }
            }
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setYear(BcdUtil.readInteger(buf, 4))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setDay(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2));
        position.setDeviceTime(dateBuilder.getDate());

        buf.readUnsignedShort(); // index
        buf.readUnsignedByte(); // tail

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        String header = buf.readCharSequence(5, StandardCharsets.UTF_8).toString();
        buf.readUnsignedShort(); // length

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        if (header.endsWith("RPT:")) {
            return decodeReport(deviceSession, buf);
        }

        return null;
    }

}
