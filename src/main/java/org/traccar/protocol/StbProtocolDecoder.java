/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;

public class StbProtocolDecoder extends BaseProtocolDecoder {

    public StbProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static class Response {
        @JsonProperty("msgType")
        private int type;
        @JsonProperty("devId")
        private String deviceId;
        @JsonProperty("result")
        private int result;
        @JsonProperty("txnNo")
        private String transaction;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        JsonObject root = Json.createReader(new StringReader((String) msg)).readObject();

        Response response = new Response();
        response.type = root.getInt("msgType");
        response.deviceId = root.getString("devId");
        response.result = 1;
        response.transaction = root.getString("txnNo");
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Context.getObjectMapper().writeValueAsString(response), remoteAddress));
        }

        return null;
    }

}
