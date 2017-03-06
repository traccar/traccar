/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.Context;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class WialonProtocol extends BaseProtocol {

    public WialonProtocol() {
        super("wialon");
        setSupportedDataCommands(
                Command.TYPE_REBOOT_DEVICE,
                Command.TYPE_SEND_USSD,
                Command.TYPE_IDENTIFICATION,
                Command.TYPE_OUTPUT_CONTROL);
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(new ServerBootstrap(), getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(4 * 1024));
                pipeline.addLast("stringEncoder", new StringEncoder());
                boolean utf8 = Context.getConfig().getBoolean(getName() + ".utf8");
                if (utf8) {
                    pipeline.addLast("stringDecoder", new StringDecoder(StandardCharsets.UTF_8));
                } else {
                    pipeline.addLast("stringDecoder", new StringDecoder());
                }
                pipeline.addLast("objectEncoder", new WialonProtocolEncoder());
                pipeline.addLast("objectDecoder", new WialonProtocolDecoder(WialonProtocol.this));
            }
        });
    }

}
