package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;

import io.netty.handler.codec.string.StringEncoder;

import java.util.List;

public class ItrProtocol extends BaseProtocol {

    public ItrProtocol() {
        addServer(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new ItrProtocolFrameDecoder());
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new ItrProtocolDecoder(ItrProtocol.this));
            }
        });
    }
}
