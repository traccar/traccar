package org.traccar.protocol;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;
import org.traccar.protocol.commands.CommandTemplate;
import org.traccar.http.commands.CommandType;

import java.util.List;
import java.util.Map;

public class AtrackProtocol extends BaseProtocol {

    public AtrackProtocol() {
        super("atrack");
    }

    @Override
    protected void loadCommandsTemplates(Map<CommandType, CommandTemplate> templates) {
        
    }

    @Override
    public void addTrackerServersTo(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(new ServerBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new AtrackFrameDecoder());
                pipeline.addLast("objectDecoder", new AtrackProtocolDecoder(AtrackProtocol.this));
            }
        });
        serverList.add(new TrackerServer(new ConnectionlessBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("objectDecoder", new AtrackProtocolDecoder(AtrackProtocol.this));
            }
        });
    }
}
