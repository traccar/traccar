/*
 * Copyright 2020 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2025 Gerrit Maus (funk@maus.xyz)
 */
package org.traccar.protocol;

import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;

import jakarta.inject.Inject;

/**
 * DCJB protocol entry point registering MQTT handlers.
 */
public class DcjbProtocol extends BaseProtocol {

    @Inject
    public DcjbProtocol(Config config) {
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config cfg) {
                pipeline.addLast(MqttEncoder.INSTANCE);
                pipeline.addLast(new MqttDecoder());
                pipeline.addLast(new DcjbProtocolDecoder(DcjbProtocol.this));
            }
        });
    }
}
