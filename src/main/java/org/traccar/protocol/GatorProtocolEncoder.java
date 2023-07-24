/*
 * Copyright 2023 - Hossain Mohammad Seym
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
import org.traccar.config.Keys;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;
import org.traccar.model.Device;

import java.nio.charset.StandardCharsets;

public class GatorProtocolEncoder extends BaseProtocolEncoder {

    public GatorProtocolEncoder(Protocol protocol) {
        super(protocol);
    }


    public static String encodeId(long deviceId) {
        StringBuilder encodedId = new StringBuilder();

        String imei = String.valueOf(deviceId);
        String a = imei.substring(1, 3);
        String b = imei.substring(3, 5);
        String c = imei.substring(5, 7);
        String d = imei.substring(7, 9);
        String e = imei.substring(9);
        String[] arr = {b, c, d, e};

        String binaryFirstDigit = Integer.toBinaryString(Integer.valueOf(a) - 30);
        binaryFirstDigit = String.format("%4s", binaryFirstDigit).replace(' ', '0');

        for (int i = 0; i < 4; i++) {
            int sum = Integer.parseInt(arr[i]) + Integer.parseInt(String.valueOf(binaryFirstDigit.charAt(i)) + "0000000", 2);
            arr[i] = Integer.toHexString(sum).toUpperCase();
            arr[i] = String.format("%2s", arr[i]).replace(' ', '0');
        }

        for (String s : arr) {
            encodedId.append(s);
        }

        return encodedId.toString();
    }

    private static String getCalibrationByteFromHexString(String data) {
        String response = "";
        int calib = 0;
        int length = data.length() / 2;
        for (int i = 0; i < length; i++) {
            calib = calib ^ Integer.parseInt(data.substring(i * 2, i * 2 + 2), 16);
        }
        response = Integer.toHexString(calib).toUpperCase();
        response = String.format("%2s", response).replace(' ', '0');
        return response;
    }

    private ByteBuf encodeContent(long deviceId, String mainOrder, String content) {
        String pseudoIPAddress = encodeId(deviceId);
        int length = 4 + 1 + 1; // ip 4 bytes, calibration byte and end byte
        String hexStringLength = String.format("%02X", length);

        String packet = "2424" + mainOrder + "00" + hexStringLength + pseudoIPAddress;
        String calibration = getCalibrationByteFromHexString(packet);
        packet = packet + calibration + "0D";
        return Unpooled.wrappedBuffer(DataConverter.parseHex(packet));
    }


    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ROLLCALL:
                return encodeContent(command.getDeviceId(), "30", null);
            default:
                return null;
        }
    }

}
