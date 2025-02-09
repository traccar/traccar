package org.traccar.protocol;

import io.netty.handler.codec.string.StringDecoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;

import jakarta.inject.Inject;

public class ItrProtocol extends BaseProtocol {

    @Inject
    public ItrProtocol(Config config) {
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new ItrProtocolDecoder(ItrProtocol.this));
            }
        });
    }

}
