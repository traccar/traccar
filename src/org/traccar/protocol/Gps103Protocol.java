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

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.CharacterDelimiterFrameDecoder;
import org.traccar.TrackerServer;
import org.traccar.command.CommandType;
import org.traccar.command.Duration;
import org.traccar.command.FixPositioningCommand;
import org.traccar.command.GpsCommand;
import org.traccar.command.CommandTemplate;
import org.traccar.command.CommandValueConversion;
import org.traccar.command.StringCommandTemplate;

import java.util.List;
import java.util.Map;

public class Gps103Protocol extends BaseProtocol {

    public Gps103Protocol() {
        super("gps103");
    }

    @Override
    protected void initCommandsTemplates(Map<CommandType, CommandTemplate> templates) {
        templates.put(CommandType.STOP_POSITIONING, new StringCommandTemplate("**,imei:[%s],A", GpsCommand.UNIQUE_ID));
        templates.put(CommandType.FIX_POSITIONING, new StringCommandTemplate("**,imei:[%s],C,[%s]", GpsCommand.UNIQUE_ID, FixPositioningCommand.FREQUENCY)
                .addConverter(Duration.class, new CommandValueConversion<Duration>() {
                    @Override
                    public String convert(Duration value) {
                        return String.format("%02d%s", value.getValue(), value.getUnit().getCommandFormat());
                    }
                }));
        templates.put(CommandType.RESUME_ENGINE, new StringCommandTemplate("**,imei:[%s],J", GpsCommand.UNIQUE_ID));
        templates.put(CommandType.STOP_ENGINE, new StringCommandTemplate("**,imei:[%s],K", GpsCommand.UNIQUE_ID));
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(new ServerBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "\r\n", "\n", ";"));
                pipeline.addLast("stringDecoder", new StringDecoder());
                pipeline.addLast("stringEncoder", new StringEncoder());
                pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(Gps103Protocol.this));
            }
        });
        serverList.add(new TrackerServer(new ConnectionlessBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("stringDecoder", new StringDecoder());
                pipeline.addLast("stringEncoder", new StringEncoder());
                pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(Gps103Protocol.this));
            }
        });
    }

}
