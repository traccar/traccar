package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;

import java.util.Map;

public class KhdProtocol extends BaseProtocol {

    public KhdProtocol() {
        super("khd");
    }

    @Override
    protected void loadCommandsDefinitions(Map<CommandType, String> commands) {
        
    }
}
