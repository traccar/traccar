package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.model.Position;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.util.List;

public class ItrProtocol extends BaseProtocol {

    public ItrProtocol() {
        super("itr");
    }

    @Override
    protected void initTrackerServers(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new ItrProtocolDecoder(ItrProtocol.this));
            }
        });
    }
}
