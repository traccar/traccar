package org.traccar.protocol;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;

import java.nio.ByteOrder;
import java.util.List;

/**
 * Created by Ivan Muratov @binakot on 11.07.2017.
 */
public class Arnavi4Protocol extends BaseProtocol {

    public Arnavi4Protocol() {
        super("arnavi4");
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        TrackerServer server = new TrackerServer(new ServerBootstrap(), getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new Arnavi4FrameDecoder());
                pipeline.addLast("objectDecoder", new Arnavi4ProtocolDecoder(Arnavi4Protocol.this));
            }
        };
        server.setEndianness(ByteOrder.LITTLE_ENDIAN);
        serverList.add(server);
    }

}
