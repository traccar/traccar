package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;
import org.traccar.PipelineBuilder;
import io.netty.channel.ChannelPipeline;

public class ItrProtocol extends BaseProtocol {

    public ItrProtocol() {
        addServer(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new ItrProtocolFrameDecoder());
                pipeline.addLast(new ItrProtocolDecoder(ItrProtocol.this));
            }
        });
    }
}
