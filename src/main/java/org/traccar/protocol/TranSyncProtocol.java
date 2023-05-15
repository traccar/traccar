package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;

import javax.inject.Inject;

public class TranSyncProtocol extends BaseProtocol {

    @Inject
    public TranSyncProtocol(Config config) {
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new TranSyncProtocolDecoder(TranSyncProtocol.this));
            }
        });
    }

}
