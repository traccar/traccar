/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;

import javax.xml.bind.DatatypeConverter;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Xt2400ProtocolDecoder extends BaseProtocolDecoder {

    public Xt2400ProtocolDecoder(Xt2400Protocol protocol) {
        super(protocol);
    }

    private Map<Integer, byte[]> formats = new HashMap<>();

    public void setConfig(String configString) {
        Pattern pattern = Pattern.compile(":wycfg pcr\\[\\d+\\] ([0-9a-fA-F]{2})[0-9a-fA-F]{2}([0-9a-fA-F]+)");
        Matcher matcher = pattern.matcher(configString);
        while (matcher.find()) {
            formats.put(Integer.parseInt(matcher.group(1)), DatatypeConverter.parseHexBinary(matcher.group(2)));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;



        return null;
    }

}
