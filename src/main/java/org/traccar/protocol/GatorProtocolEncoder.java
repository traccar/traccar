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

import java.nio.charset.StandardCharsets;

public class GatorProtocolEncoder extends BaseProtocolEncoder {

    public GatorProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private ByteBuf engineExecute(String Pseudo_ID, byte engineState){
        ByteBuf buf = Unpooled.buffer(256);

        // Add Header - 24 24
        buf.writeByte(0x24);
        buf.writeByte(0x24);

        // Add Main Order - 0x38 for Engine Start, 0x39 for Engine Stop
        buf.writeByte(engineState);

        // Add Packet Length - 00 06
        buf.writeByte(0x00);
        buf.writeByte(0x06);

        // Add Device ID - String to 5 Byte
        // Convert String to Int Array
        int[] _ip = numToIp(Pseudo_ID);

        buf.writeByte(_ip[0]);
        buf.writeByte(_ip[1]);
        buf.writeByte(_ip[2]);
        buf.writeByte(_ip[3]);

//        String _decoded_id = decodeId(_ip[0], _ip[1], _ip[2], _ip[3]);
//        LOGGER.info("Decoded ID: " + _decoded_id);

//        // CRC / Calibration -> XOR All Bytes
//        byte _crc = 0;
//        for (int i = 0; i < buf.writerIndex(); i++) {
//            _crc ^= buf.getByte(i);
//        }
//
//        // Add CRC
//        buf.writeByte(_crc);
        buf.writeByte(Checksum.xor(buf.nioBuffer(2, buf.writerIndex())));

        // End of Packet -> 0D
        buf.writeByte(0x0D);

        return buf;
    }

//    public static String decodeId(int b1, int b2, int b3, int b4) {
//
//        int d1 = 30 + ((b1 >> 7) << 3) + ((b2 >> 7) << 2) + ((b3 >> 7) << 1) + (b4 >> 7);
//        int d2 = b1 & 0x7f;
//        int d3 = b2 & 0x7f;
//        int d4 = b3 & 0x7f;
//        int d5 = b4 & 0x7f;
//
//        return String.format("%02d%02d%02d%02d%02d", d1, d2, d3, d4, d5);
//    }

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
        if ((iHigt & 0x08) != 0)
            sIp[0] = Integer.parseInt(temp[0]) | 128;
        else
            sIp[0] = Integer.parseInt(temp[0]);
        if ((iHigt & 0x04) != 0)
            sIp[1] = Integer.parseInt(temp[1]) | 128;
        else
            sIp[1] = Integer.parseInt(temp[1]);
        if ((iHigt & 0x02) != 0)
            sIp[2] = Integer.parseInt(temp[2]) | 128;
        else
            sIp[2] = Integer.parseInt(temp[2]);
        if ((iHigt & 0x01) != 0)
            sIp[3] = Integer.parseInt(temp[3]) | 128;
        else
            sIp[3] = Integer.parseInt(temp[3]);
        return sIp;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ENGINE_RESUME:
                LOGGER.info("Command: " + Command.TYPE_ENGINE_RESUME);
                return engineExecute(getUniqueId(command.getDeviceId()), (byte) 0x38);
            case Command.TYPE_ENGINE_STOP:
                LOGGER.info("Command: " + Command.TYPE_ENGINE_STOP);
                return engineExecute(getUniqueId(command.getDeviceId()), (byte) 0x39);
            default:
                return null;
        }
    }

}