/*
 * Copyright 2024 - 2025 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

import jakarta.inject.Inject;

public class ES4x0Protocol extends BaseProtocol {

    @Inject
    public ES4x0Protocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_CUSTOM,
                Command.TYPE_POSITION_SINGLE,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_REBOOT_DEVICE,
                Command.TYPE_GET_VERSION,
                Command.TYPE_GET_DEVICE_STATUS,
                Command.TYPE_SET_ODOMETER,
                Command.TYPE_OUTPUT_CONTROL,
                "interval",
                "nomove",
                "speedalarm",
                "apn",
                "heartbeat",
                "sleepmode",
                "data");

        setSupportedTextCommands(
                Command.TYPE_CUSTOM,
                Command.TYPE_POSITION_SINGLE,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_REBOOT_DEVICE,
                Command.TYPE_GET_VERSION);

        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new ES4x0FrameDecoder());
                pipeline.addLast(new ES4x0ProtocolEncoder(ES4x0Protocol.this));
                pipeline.addLast(new ES4x0ProtocolDecoder(ES4x0Protocol.this));
            }
        });
        
        addServer(new TrackerServer(config, getName(), true) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new ES4x0FrameDecoder());
                pipeline.addLast(new ES4x0ProtocolEncoder(ES4x0Protocol.this));
                pipeline.addLast(new ES4x0ProtocolDecoder(ES4x0Protocol.this));
            }
        });

        setTextCommandEncoder(new ES4x0ProtocolEncoder(this));
    }

}
