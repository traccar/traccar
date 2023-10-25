/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;


public class ZrProtocolEncoder extends BaseProtocolEncoder {

    public ZrProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {
        ByteBuf id = Unpooled.wrappedBuffer(DataConverter.parseHex(strHexPaddingLeft(getUniqueId(command.getDeviceId()), 20)));
        switch (command.getType()) {
            case Command.TYPE_SET_CONNECTION:
                return formatSetConnectionCommand(id, command);
            case Command.TYPE_ALARM_SPEED:
                break;
        }
        return null;
    }

    private static ByteBuf formatSetConnectionCommand(ByteBuf id, Command command) {
        ByteBuf request = Unpooled.buffer();
        ZrProtocolDecoder.add2391(request);

        request.writeShort(0x24c0);

        ByteBuf setConnectionCmd = Unpooled.buffer();
        // 3: tcp long connection
        setConnectionCmd.writeByte(3);
        setConnectionCmd.writeByte(2);

        // (N+M)*2
        setConnectionCmd.writeByte(1);
        setConnectionCmd.writeShort(command.getInteger(Command.KEY_PORT));
        setConnectionCmd.writeByte(command.getString(Command.KEY_SERVER).getBytes().length);
        setConnectionCmd.writeBytes(command.getString(Command.KEY_SERVER).getBytes());

        setConnectionCmd.writeByte(0);

        // sms gateway
        setConnectionCmd.writeByte(0);

        setConnectionCmd.writeInt(300);

        request.writeShort(setConnectionCmd.readableBytes());
        request.writeBytes(setConnectionCmd);

        return ZrProtocolDecoder.formatMessage(ZrProtocolDecoder.MSG_CFG, id, (short) 0x1040, (byte) 0, 1, request);
    }


    /**
     * If there are insufficient digits, add 0 from the left.
     * If the data parameter is given as 3242,and the targetLength parameter is set to 6,
     * the function will return 003242.
     *
     * @param data:data
     * @param targetLength:targetLength
     */
    public static String strHexPaddingLeft(String data, int targetLength) {
        int dataLength = data.length();
        if (dataLength < targetLength) {
            StringBuilder dataBuilder = new StringBuilder(data);
            for (int i = dataLength; i < targetLength; i++) {
                dataBuilder.insert(0, "0");
            }
            data = dataBuilder.toString();
        }
        return data;
    }

}
