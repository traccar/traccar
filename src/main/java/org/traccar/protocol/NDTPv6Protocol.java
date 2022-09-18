/*
 * 2020 - NDTP v6 Protocol
 */
package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;

import javax.inject.Inject;

public class NDTPv6Protocol extends BaseProtocol {

    @Inject
    public NDTPv6Protocol(Config config) {
        addServer(
                new TrackerServer(config, getName(), false) {
                    @Override
                    protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                        pipeline.addLast(new NDTPv6ProtocolDecoder(NDTPv6Protocol.this));
                    }
                }
        );
    }
}
