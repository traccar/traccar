package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.Config;
import io.netty.channel.ChannelPipeline;

public class ItrProtocol extends BaseProtocol {

    public ItrProtocol(Config config) {
        super(config);
        addServer(new BaseProtocol.Server(this, false, new PipelineBuilder() {
            @Override
            protected void addHandlers(ChannelPipeline pipeline) {
                pipeline.addLast(new ItrProtocolFrameDecoder());
                pipeline.addLast(new ItrProtocolDecoder(ItrProtocol.this));
            }
        }));
    }
}
