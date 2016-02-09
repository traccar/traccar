/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;

import java.util.List;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.traccar.model.Command;

public class H02Protocol extends BaseProtocol {

    public H02Protocol() {
        super("h02");
        setSupportedCommands(
                Command.TYPE_POSITION_STOP,
                Command.TYPE_POSITION_SINGLE,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_ALARM_ARM,
                Command.TYPE_ALARM_DISARM,
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_ALARM_DOOR_ARM,
                Command.TYPE_ALARM_DOOR_DISARM,
                Command.TYPE_ALARM_IGNITION_ARM,
                Command.TYPE_ALARM_IGNITION_DISARM,
                Command.TYPE_MOVEMENT_ALARM,
                Command.TYPE_ALARM_POSITION_DISARM
                );
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(new ServerBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new H02FrameDecoder());
                pipeline.addLast("objectDecoder", new H02ProtocolDecoder(H02Protocol.this));
                pipeline.addLast("stringEncoder", new StringEncoder());         
                pipeline.addLast("objectEncoder", new H02ProtocolEncoder());
            }
        });
    }

}
