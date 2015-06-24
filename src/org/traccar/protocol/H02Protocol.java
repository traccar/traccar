package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;

import java.util.Map;

public class H02Protocol extends BaseProtocol {

    public H02Protocol() {
        super("h02");
    }

    @Override
    protected void loadCommandsDefinitions(Map<CommandType, String> commands) {
        
    }
}
