/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerClient;
import org.traccar.config.Config;

import javax.inject.Inject;

public class OrbcommProtocol extends BaseProtocol {

    @Inject
    public OrbcommProtocol(Config config) {
        addClient(new TrackerClient(config, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new HttpRequestEncoder());
                pipeline.addLast(new HttpResponseDecoder());
                pipeline.addLast(new HttpObjectAggregator(65535));
                pipeline.addLast(new OrbcommProtocolDecoder(OrbcommProtocol.this));
                pipeline.addLast(new OrbcommProtocolPoller(OrbcommProtocol.this, config));
            }
        });
    }

}
