/*
 * Copyright 2015 - 2020 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Command;

import javax.inject.Inject;

public class XexunProtocol extends BaseProtocol {

    @Inject
    public XexunProtocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                boolean full = config.getBoolean(Keys.PROTOCOL_EXTENDED.withPrefix(getName()));
                if (full) {
                    pipeline.addLast(new LineBasedFrameDecoder(1024)); // tracker bug \n\r
                } else {
                    pipeline.addLast(new XexunFrameDecoder());
                }
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new XexunProtocolEncoder(XexunProtocol.this));
                pipeline.addLast(new XexunProtocolDecoder(XexunProtocol.this, full));
            }
        });
    }

}
