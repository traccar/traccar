package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;

import java.util.Map;

public class CastelProtocol extends BaseProtocol {

    public CastelProtocol() {
        super("castel");
    }

    @Override
    protected void loadCommandsDefinitions(Map<CommandType, String> commands) {
        
    }
}
