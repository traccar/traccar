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
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;

public class NdtpV6ProtocolDecoder extends BaseProtocolDecoder {

    public NdtpV6ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final byte[] SIGNATURE = {0x7E, 0x7E};

    private static final int NPL_FLAG_CRC = 2;
    private static final int NPH_RESULT_OK = 0x00000000;
    private static final int NPL_TYPE_NPH = 2;
    private static final int NPL_ADDRESS_SERVER = 0;

    private static final int NPH_RESULT = 0;

    private static final int NPH_SRV_GENERIC_CONTROLS = 0;
    private static final int NPH_SRV_NAVDATA = 1;

    private static final int NPH_SGC_RESULT = NPH_RESULT;
    private static final int NPH_SGC_CONN_REQUEST = 100;

    private static final int NPH_SND_RESULT = NPH_RESULT;

    private void sendResponse(
            Channel channel, int serviceId, long requestId) {

        ByteBuf content = Unpooled.buffer();
        content.writeShortLE(serviceId);
        content.writeIntLE(NPH_SND_RESULT);
        content.writeIntLE((int) requestId);
        content.writeIntLE(NPH_RESULT_OK);

        ByteBuf response = Unpooled.buffer();
        response.writeBytes(SIGNATURE);
        response.writeShortLE(content.readableBytes());
        response.writeShortLE(NPL_FLAG_CRC); // flags
        response.writeShort(Checksum.crc16(Checksum.CRC16_MODBUS, content.nioBuffer()));
        response.writeByte(NPL_TYPE_NPH); // type
        response.writeIntLE(NPL_ADDRESS_SERVER); // peer address
        response.writeShortLE(0); // request id
        response.writeBytes(content);
        content.release();

        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
    }

    private static final short MAIN_NAV_DATA = 0;
    private static final short ADDITIONAL_NAV_DATA = 2;

    private void decodeData(ByteBuf buf, Position position) {

        short itemType;
        short itemIndex;

        itemType = buf.readUnsignedByte();
        itemIndex = buf.readUnsignedByte();
        if (itemType == MAIN_NAV_DATA && (itemIndex == 0 || itemIndex == 1)) {

            position.setTime(new Date(buf.readUnsignedIntLE() * 1000));
            position.setLongitude(buf.readIntLE() / 10000000.0);
            position.setLatitude(buf.readIntLE() / 10000000.0);

            short flags = buf.readUnsignedByte();
            position.setValid(BitUtil.check(flags, 7));
            if (BitUtil.check(flags, 1)) {
                position.addAlarm(Position.ALARM_GENERAL);
            }

            position.set(Position.KEY_BATTERY, buf.readUnsignedByte() * 20);
            position.set(Position.KEY_OBD_SPEED, buf.readUnsignedShortLE());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
            position.setCourse(buf.readUnsignedShortLE());

            position.set(Position.KEY_ODOMETER, buf.readUnsignedShortLE());
            position.setAltitude(buf.readShortLE());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.set(Position.KEY_PDOP, buf.readUnsignedByte());
        }

        itemType = buf.readUnsignedByte();
        itemIndex = buf.readUnsignedByte();
        if (itemType == ADDITIONAL_NAV_DATA && itemIndex == 0) {

            position.set(Position.KEY_BATTERY_LEVEL, Math.max((buf.readUnsignedShortLE() - 3600) / 6, 100));
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShortLE());
            position.set(Position.PREFIX_ADC + 3, buf.readUnsignedShortLE());
            position.set(Position.PREFIX_ADC + 4, buf.readUnsignedShortLE());

            buf.readUnsignedByte(); // inputs
            buf.readUnsignedByte(); // outputs
            buf.readUnsignedShortLE(); // in1 count
            buf.readUnsignedShortLE(); // in2 count
            buf.readUnsignedShortLE(); // in3 count
            buf.readUnsignedShortLE(); // in4 count
            buf.readUnsignedIntLE(); // track length

            position.set(Position.KEY_ANTENNA, buf.readUnsignedByte());
            position.set(Position.KEY_GPS, buf.readUnsignedByte());
            position.set(Position.KEY_ACCELERATION, buf.readUnsignedByte());
            position.set(Position.KEY_POWER, buf.readUnsignedByte() * 200);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

        buf.skipBytes(2); // signature
        buf.readUnsignedShortLE(); // length
        buf.readUnsignedShortLE(); // connection flags
        buf.readUnsignedShortLE(); // checksum
        buf.readUnsignedByte(); // type
        buf.readUnsignedIntLE(); // address
        buf.readUnsignedShortLE(); // identification

        int serviceId = buf.readUnsignedShortLE();
        int serviceType = buf.readUnsignedShortLE();
        buf.readUnsignedShortLE(); // request flags
        long requestId = buf.readUnsignedIntLE();

        if (deviceSession == null && serviceId == NPH_SRV_GENERIC_CONTROLS && serviceType == NPH_SGC_CONN_REQUEST) {

            buf.readUnsignedShortLE(); // version major
            buf.readUnsignedShortLE(); // version minor
            buf.readUnsignedShortLE(); // connection flags

            int deviceId = buf.readUnsignedShortLE();
            Position position = new Position(getProtocolName());
            deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(deviceId));
            position.setDeviceId(deviceSession.getDeviceId());

            if (channel != null) {
                sendResponse(channel, serviceId, requestId);
            }

            position.set(Position.KEY_RESULT, String.valueOf(NPH_SGC_RESULT));
            position.setTime(new Date());
            getLastLocation(position, new Date());
            position.setValid(false);

            return position;

        }

        if (serviceId == NPH_SRV_NAVDATA) {

            deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            if (channel != null) {
                sendResponse(channel, serviceId, requestId);
            }

            decodeData(buf, position);

            return position;
        }

        return null;
    }

}
