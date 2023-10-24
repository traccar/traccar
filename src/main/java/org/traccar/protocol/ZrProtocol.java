package org.traccar.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import jakarta.inject.Inject;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

/**
 * @author QingtaiJiang
 * @date 2023/9/18 16:53
 * @email qingtaij@163.com
 */
public class ZrProtocol extends BaseProtocol {
    @Inject
    public ZrProtocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_SET_CONNECTION);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new LengthFieldBasedFrameDecoder(2048, 2, 2, 2, 0));
                pipeline.addLast(new ZrProtocolDecoder(ZrProtocol.this));
                pipeline.addLast(new ZrProtocolEncoder(ZrProtocol.this));
            }
        });
    }
}
