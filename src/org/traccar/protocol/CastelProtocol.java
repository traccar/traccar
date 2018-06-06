/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

import java.nio.ByteOrder;
import java.util.List;

public class CastelProtocol extends BaseProtocol {

    public CastelProtocol() {
        super("castel");
        setSupportedDataCommands(
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME);
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast("frameDecoder",
                        new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, 1024, 2, 2, -4, 0, true));
                pipeline.addLast("objectEncoder", new CastelProtocolEncoder());
                pipeline.addLast("objectDecoder", new CastelProtocolDecoder(CastelProtocol.this));
            }
        });
        serverList.add(new TrackerServer(true, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast("objectEncoder", new CastelProtocolEncoder());
                pipeline.addLast("objectDecoder", new CastelProtocolDecoder(CastelProtocol.this));
            }
        });
    }

}
