/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HuabaoProtocolEncoder extends BaseProtocolEncoder {

    @Override
    protected Object encodeCommand(Command command) {

        ByteBuf id = Unpooled.wrappedBuffer(
                DataConverter.parseHex(getUniqueId(command.getDeviceId())));
        try {
            ByteBuf data = Unpooled.buffer();
            byte[] time = DataConverter.parseHex(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));

            switch (command.getType()) {
                case Command.TYPE_ENGINE_STOP:
                    data.writeByte(0x01);
                    data.writeBytes(time);
                    return HuabaoProtocolDecoder.formatMessage(HuabaoProtocolDecoder.MSG_OIL_CONTROL, id, data);
                case Command.TYPE_ENGINE_RESUME:
                    data.writeByte(0x00);
                    data.writeBytes(time);
                    return HuabaoProtocolDecoder.formatMessage(HuabaoProtocolDecoder.MSG_OIL_CONTROL, id, data);
                default:
                    return null;
            }
        } finally {
            id.release();
        }
    }

}
