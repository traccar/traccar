/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

import java.nio.ByteOrder;
import java.util.List;

public class Pt502Protocol extends BaseProtocol {

    public Pt502Protocol() {
        super("pt502");
        setSupportedDataCommands(
                Command.TYPE_CUSTOM,
                Command.TYPE_SET_TIMEZONE,
                Command.TYPE_ALARM_SPEED,
                Command.TYPE_OUTPUT_CONTROL,
                Command.TYPE_REQUEST_PHOTO);
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        TrackerServer server = new TrackerServer(new ServerBootstrap(), getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new Pt502FrameDecoder());
                pipeline.addLast("stringEncoder", new StringEncoder());
                pipeline.addLast("stringDecoder", new StringDecoder());
                pipeline.addLast("objectEncoder", new Pt502ProtocolEncoder());
                pipeline.addLast("objectDecoder", new Pt502ProtocolDecoder(Pt502Protocol.this));
            }
        };
        server.setEndianness(ByteOrder.LITTLE_ENDIAN);
        serverList.add(server);
    }

}
