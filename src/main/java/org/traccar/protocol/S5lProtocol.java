package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

import jakarta.inject.Inject;

public class S5lProtocol extends BaseProtocol {

    @Inject
    public S5lProtocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_POSITION_SINGLE,
                Command.TYPE_REBOOT_DEVICE,
                Command.TYPE_FACTORY_RESET,
                Command.TYPE_CUSTOM);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new S5lFrameDecoder());
                pipeline.addLast(new Gt06ProtocolEncoder(S5lProtocol.this)); // Using Gt06ProtocolEncoder for S5l
                pipeline.addLast(new S5lProtocolDecoder(S5lProtocol.this));
            }
        });
    }
}
