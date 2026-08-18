/*
 * Copyright 2026 Mateus Azevedo (financeiro@akroztelematics.com.br)
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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;
import org.traccar.PipelineBuilder;
import org.traccar.config.Config;
import org.traccar.model.Command;

@Singleton
public class AkrozProtocol extends BaseProtocol {

    @Inject
    public AkrozProtocol(Config config) {

        setSupportedDataCommands(Command.TYPE_CUSTOM);

        addServer(new TrackerServer(config, getName(), true) {

            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new AkrozProtocolDecoder(AkrozProtocol.this));
                pipeline.addLast(new AkrozProtocolEncoder(AkrozProtocol.this));
            }
        });
    }
}
