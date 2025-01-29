package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

import java.util.List;

public class ItrProtocol extends BaseProtocol {

    public ItrProtocol() {
        // Adiciona um servidor para dispositivos iTR
        addServer(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new ItrProtocolFrameDecoder()); // Decodifica os quadros de dados
                pipeline.addLast(new ItrProtocolEncoder()); // Codifica comandos para o dispositivo
                pipeline.addLast(new ItrProtocolDecoder(ItrProtocol.this)); // Decodifica pacotes de dados
            }
        });
    }
}
