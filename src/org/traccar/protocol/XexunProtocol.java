package org.traccar.protocol;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.traccar.BaseProtocol;
import org.traccar.Context;
import org.traccar.TrackerServer;
import org.traccar.protocol.commands.CommandTemplate;
import org.traccar.http.commands.CommandType;

import java.util.List;
import java.util.Map;

public class XexunProtocol extends BaseProtocol {

    public XexunProtocol() {
        super("xexun");
    }

    @Override
    protected void loadCommandsTemplates(Map<CommandType, CommandTemplate> templates) {
        
    }

    @Override
    public void addTrackerServersTo(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(new ServerBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                if (Boolean.valueOf(Context.getProps().getProperty(XexunProtocol.this.getName() + ".extended"))) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024)); // tracker bug \n\r
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Xexun2ProtocolDecoder(XexunProtocol.this));
                } else {
                    pipeline.addLast("frameDecoder", new XexunFrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new XexunProtocolDecoder(XexunProtocol.this));
                }
            }
        });
    }
}
