package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;

import java.util.Map;

public class ApelProtocol extends BaseProtocol {

    public ApelProtocol() {
        super("apel");
    }

    @Override
    protected void loadCommandsDefinitions(Map<CommandType, String> commands) {
        
    }
}
