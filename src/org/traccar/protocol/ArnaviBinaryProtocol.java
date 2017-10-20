package org.traccar.protocol;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;

import java.nio.ByteOrder;
import java.util.List;

public class ArnaviBinaryProtocol extends BaseProtocol {
    public ArnaviBinaryProtocol() {
        super("arnavibin");
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        TrackerServer server = new TrackerServer(new ServerBootstrap(), getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
//                pipeline.addLast("frameDecoder", new ArnaviBinaryFrameDecoder());
//                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 2, 1, 2, 0));
                pipeline.addLast("objectDecoder", new ArnaviBinaryProtocolDecoder(ArnaviBinaryProtocol.this));
            }
        };
        server.setEndianness(ByteOrder.LITTLE_ENDIAN);
        serverList.add(server);

    }

}
