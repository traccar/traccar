/*
 * Copyright 2025 - 2026 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

import jakarta.inject.Inject;

public class JmakProtocol extends BaseProtocol {

    @Inject
    public JmakProtocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_CUSTOM,
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_OUTPUT_CONTROL,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_SET_ODOMETER,
                Command.TYPE_SET_CONNECTION,
                Command.TYPE_REBOOT_DEVICE,
                Command.TYPE_SET_SPEED_LIMIT,
                Command.TYPE_ALARM_BATTERY,
                Command.TYPE_ALARM_REMOVE,
                Command.TYPE_ALARM_VIBRATION);
        setTextCommandEncoder(new JmakProtocolSmsEncoder(this));
        setSupportedTextCommands(
                Command.TYPE_CUSTOM,
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_OUTPUT_CONTROL,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_SET_ODOMETER,
                Command.TYPE_SET_CONNECTION,
                Command.TYPE_REBOOT_DEVICE,
                Command.TYPE_SET_SPEED_LIMIT,
                Command.TYPE_ALARM_BATTERY,
                Command.TYPE_ALARM_REMOVE,
                Command.TYPE_ALARM_VIBRATION);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new JmakFrameDecoder());
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new JmakProtocolEncoder(JmakProtocol.this));
                pipeline.addLast(new JmakProtocolDecoder(JmakProtocol.this));
            }
        });
        addServer(new TrackerServer(config, getName(), true) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new JmakProtocolEncoder(JmakProtocol.this));
                pipeline.addLast(new JmakProtocolDecoder(JmakProtocol.this));
            }
        });
    }

}
