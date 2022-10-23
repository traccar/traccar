/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DataConverter;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HuabaoProtocolEncoder extends BaseProtocolEncoder {

    public HuabaoProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_ALTERNATIVE.withPrefix(getProtocolName()), command.getDeviceId());

        ByteBuf id = Unpooled.wrappedBuffer(
                DataConverter.parseHex(getUniqueId(command.getDeviceId())));
        try {
            ByteBuf data = Unpooled.buffer();
            byte[] time = DataConverter.parseHex(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));

            switch (command.getType()) {
                case Command.TYPE_ENGINE_STOP:
                    if (alternative) {
                        data.writeByte(0x01);
                        data.writeBytes(time);
                        return HuabaoProtocolDecoder.formatMessage(
                                HuabaoProtocolDecoder.MSG_OIL_CONTROL, id, false, data);
                    } else {
                        data.writeByte(0xf0);
                        return HuabaoProtocolDecoder.formatMessage(
                                HuabaoProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                    }
                case Command.TYPE_ENGINE_RESUME:
                    if (alternative) {
                        data.writeByte(0x00);
                        data.writeBytes(time);
                        return HuabaoProtocolDecoder.formatMessage(
                                HuabaoProtocolDecoder.MSG_OIL_CONTROL, id, false, data);
                    } else {
                        data.writeByte(0xf1);
                        return HuabaoProtocolDecoder.formatMessage(
                                HuabaoProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                    }
                default:
                    return null;
            }
        } finally {
            id.release();
        }
    }

}
