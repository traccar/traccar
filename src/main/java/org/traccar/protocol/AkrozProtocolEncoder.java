/*
 * Copyright 2026 Mateus Azevedo (financeiro@akroztelematics.com.br)
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

import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class AkrozProtocolEncoder extends BaseProtocolEncoder {

    private final AtomicInteger counter = new AtomicInteger(0x8000);

    public AkrozProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        String data = command.getString(Command.KEY_DATA);
        if (data == null) {
            return null;
        }

        String message = String.format(
                ">%s;ID=%s;#%04X;*",
                data, getUniqueId(command.getDeviceId()), counter.getAndIncrement() & 0xFFFF);
        message += String.format("%02X", checksum(message)) + "<";

        return Unpooled.copiedBuffer(message, StandardCharsets.US_ASCII);
    }

    private int checksum(String data) {
        int checksum = 0;
        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) == '*' && i > 0 && data.charAt(i - 1) == ';') {
                break;
            }
            checksum ^= data.charAt(i);
        }
        return checksum & 0xFF;
    }

}
