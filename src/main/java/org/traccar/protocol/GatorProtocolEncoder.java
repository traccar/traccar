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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Main;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class GatorProtocolEncoder extends BaseProtocolEncoder {

    public GatorProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static final int GET_POSITION = 0x30;
    public static final int SET_ENGINE_START = 0x38;
    public static final int SET_ENGINE_STOP = 0x39;


    private ByteBuf engineExecute(String pseudoAddress, int engineState) {
        ByteBuf buf = Unpooled.buffer(256);

        // Add Header - 24 24
        buf.writeByte(0x24);
        buf.writeByte(0x24);

        // Add Main Order - 0x38 for Engine Start, 0x39 for Engine Stop
        buf.writeByte(engineState);

        // Add Packet Length - 00 06
        buf.writeByte(0x00);
        buf.writeByte(0x06);

        // Add Device ID
        int[] ipAddress = numToIp(pseudoAddress);

        buf.writeByte(ipAddress[0]);
        buf.writeByte(ipAddress[1]);
        buf.writeByte(ipAddress[2]);
        buf.writeByte(ipAddress[3]);

        // Add Checksum
        buf.writeByte(Checksum.xor(buf.nioBuffer(2, buf.writerIndex())));

        // End of Packet -> 0D
        buf.writeByte(0x0D);

        return buf;
    }

    private ByteBuf getFromDevice(String pseudoAddress, int type) {
        ByteBuf buf = Unpooled.buffer(256);

        // Add Header - 24 24
        buf.writeByte(0x24);
        buf.writeByte(0x24);

        // Add Type
        buf.writeByte(type);

        // Add Packet Length - 00 06
        buf.writeByte(0x00);
        buf.writeByte(0x06);

        // Add Device ID
        int[] ipAddress = numToIp(pseudoAddress);

        buf.writeByte(ipAddress[0]);
        buf.writeByte(ipAddress[1]);
        buf.writeByte(ipAddress[2]);
        buf.writeByte(ipAddress[3]);

        // Add Checksum
        buf.writeByte(Checksum.xor(buf.nioBuffer(2, buf.writerIndex())));

        // End of Packet -> 0D
        buf.writeByte(0x0D);

        return buf;
    }

    public int[] numToIp(String sim) {
        String[] temp = new String[4];
        int iHigt;
        switch (sim.length()) {
            case 11:
                temp[0] = sim.substring(3, 5);
                temp[1] = sim.substring(5, 7);
                temp[2] = sim.substring(7, 9);
                temp[3] = sim.substring(9, 11);
                iHigt = Integer.parseInt(sim.substring(1, 3)) - 30;
                break;
            case 10:
                temp[0] = sim.substring(2, 4);
                temp[1] = sim.substring(4, 6);
                temp[2] = sim.substring(6, 8);
                temp[3] = sim.substring(8, 10);
                iHigt = Integer.parseInt(sim.substring(0, 2)) - 30;
                break;
            case 9:
                temp[0] = sim.substring(1, 3);
                temp[1] = sim.substring(3, 5);
                temp[2] = sim.substring(5, 7);
                temp[3] = sim.substring(7, 9);
                iHigt = Integer.parseInt(sim.substring(0, 1));
                break;
            default:
                switch (sim.length()) {
                    case 8:
                        return numToIp("140" + sim);
                    case 7:
                        return numToIp("1400" + sim);
                    case 6:
                        return numToIp("14000" + sim);
                    case 5:
                        return numToIp("140000" + sim);
                    case 4:
                        return numToIp("1400000" + sim);
                    case 3:
                        return numToIp("14000000" + sim);
                    case 2:
                        return numToIp("140000000" + sim);
                    case 1:
                        return numToIp("1400000000" + sim);
                    default:
                        return new int[4];
                }
        }
        int[] sIp = new int[4];
        if ((iHigt & 0x08) != 0) {
            sIp[0] = Integer.parseInt(temp[0]) | 128;
        } else {
            sIp[0] = Integer.parseInt(temp[0]);
        }
        if ((iHigt & 0x04) != 0) {
            sIp[1] = Integer.parseInt(temp[1]) | 128;
        } else {
                sIp[1] = Integer.parseInt(temp[1]);
        }
        if ((iHigt & 0x02) != 0) {
            sIp[2] = Integer.parseInt(temp[2]) | 128;
        } else {
            sIp[2] = Integer.parseInt(temp[2]);
        }
        if ((iHigt & 0x01) != 0) {
            sIp[3] = Integer.parseInt(temp[3]) | 128;
        } else {
            sIp[3] = Integer.parseInt(temp[3]);
        }
        return sIp;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ENGINE_RESUME:
                LOGGER.info("Command: " + Command.TYPE_ENGINE_RESUME);
                return engineExecute(getUniqueId(command.getDeviceId()), SET_ENGINE_START);
            case Command.TYPE_ENGINE_STOP:
                LOGGER.info("Command: " + Command.TYPE_ENGINE_STOP);
                return engineExecute(getUniqueId(command.getDeviceId()), SET_ENGINE_STOP);
            case Command.TYPE_POSITION_SINGLE:
                LOGGER.info("Command: " + Command.TYPE_POSITION_SINGLE);
                return getFromDevice(getUniqueId(command.getDeviceId()), GET_POSITION);
            default:
                return null;
        }
    }

}
