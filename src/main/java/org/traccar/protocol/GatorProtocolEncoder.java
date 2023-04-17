/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class GatorProtocolEncoder extends BaseProtocolEncoder {

    public GatorProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    public static final int GET_POSITION = 0x30;
    public static final int SET_ENGINE_START = 0x38;
    public static final int SET_ENGINE_STOP = 0x39;


    private ByteBuf engineExecute(String pseudoAddress, int engineState) {
        ByteBuf buf = Unpooled.buffer(256);

        buf.writeShort(0x2424);

        buf.writeByte(engineState);

        buf.writeShort(0x0006); // Length

        int[] ipAddress = idToPseudoIPAddress(pseudoAddress);

        buf.writeByte(ipAddress[0]);
        buf.writeByte(ipAddress[1]);
        buf.writeByte(ipAddress[2]);
        buf.writeByte(ipAddress[3]);

        buf.writeByte(Checksum.xor(buf.nioBuffer(2, buf.writerIndex())));

        buf.writeByte(0x0D);

        return buf;
    }

    private ByteBuf getFromDevice(String pseudoAddress, int type) {
        ByteBuf buf = Unpooled.buffer(256);

        buf.writeShort(0x2424);

        buf.writeByte(type);

        buf.writeShort(0x0006); // Length

        int[] ipAddress = idToPseudoIPAddress(pseudoAddress);

        buf.writeByte(ipAddress[0]);
        buf.writeByte(ipAddress[1]);
        buf.writeByte(ipAddress[2]);
        buf.writeByte(ipAddress[3]);

        buf.writeByte(Checksum.xor(buf.nioBuffer(2, buf.writerIndex())));

        buf.writeByte(0x0D);

        return buf;
    }

    public int[] idToPseudoIPAddress(String deviceID) {
        String[] ipAddressString = new String[4];
        int highOrderBits;

        switch (deviceID.length()) {
            case 11:
                ipAddressString[0] = deviceID.substring(3, 5);
                ipAddressString[1] = deviceID.substring(5, 7);
                ipAddressString[2] = deviceID.substring(7, 9);
                ipAddressString[3] = deviceID.substring(9, 11);
                highOrderBits = Integer.parseInt(deviceID.substring(1, 3)) - 30;
                break;
            case 10:
                ipAddressString[0] = deviceID.substring(2, 4);
                ipAddressString[1] = deviceID.substring(4, 6);
                ipAddressString[2] = deviceID.substring(6, 8);
                ipAddressString[3] = deviceID.substring(8, 10);
                highOrderBits = Integer.parseInt(deviceID.substring(0, 2)) - 30;
                break;
            case 9:
                ipAddressString[0] = deviceID.substring(1, 3);
                ipAddressString[1] = deviceID.substring(3, 5);
                ipAddressString[2] = deviceID.substring(5, 7);
                ipAddressString[3] = deviceID.substring(7, 9);
                highOrderBits = Integer.parseInt(deviceID.substring(0, 1));
                break;
            default:
                switch (deviceID.length()) {
                    case 8:
                        return idToPseudoIPAddress("140" + deviceID);
                    case 7:
                        return idToPseudoIPAddress("1400" + deviceID);
                    case 6:
                        return idToPseudoIPAddress("14000" + deviceID);
                    case 5:
                        return idToPseudoIPAddress("140000" + deviceID);
                    case 4:
                        return idToPseudoIPAddress("1400000" + deviceID);
                    case 3:
                        return idToPseudoIPAddress("14000000" + deviceID);
                    case 2:
                        return idToPseudoIPAddress("140000000" + deviceID);
                    case 1:
                        return idToPseudoIPAddress("1400000000" + deviceID);
                    default:
                        return new int[4];
                }
        }

        int[] ipAddress = new int[4];

        if ((highOrderBits & 0x08) != 0) {
            ipAddress[0] = Integer.parseInt(ipAddressString[0]) | 128;
        } else {
            ipAddress[0] = Integer.parseInt(ipAddressString[0]);
        }
        if ((highOrderBits & 0x04) != 0) {
            ipAddress[1] = Integer.parseInt(ipAddressString[1]) | 128;
        } else {
            ipAddress[1] = Integer.parseInt(ipAddressString[1]);
        }
        if ((highOrderBits & 0x02) != 0) {
            ipAddress[2] = Integer.parseInt(ipAddressString[2]) | 128;
        } else {
            ipAddress[2] = Integer.parseInt(ipAddressString[2]);
        }
        if ((highOrderBits & 0x01) != 0) {
            ipAddress[3] = Integer.parseInt(ipAddressString[3]) | 128;
        } else {
            ipAddress[3] = Integer.parseInt(ipAddressString[3]);
        }

        return ipAddress;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ENGINE_RESUME:
                return engineExecute(getUniqueId(command.getDeviceId()), SET_ENGINE_START);
            case Command.TYPE_ENGINE_STOP:
                return engineExecute(getUniqueId(command.getDeviceId()), SET_ENGINE_STOP);
            case Command.TYPE_POSITION_SINGLE:
                return getFromDevice(getUniqueId(command.getDeviceId()), GET_POSITION);
            default:
                return null;
        }
    }

}
