package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;

import java.util.Map;

public class Tk102Protocol extends BaseProtocol {

    public Tk102Protocol() {
        super("tk102");
    }

    @Override
    protected void loadCommandsDefinitions(Map<CommandType, String> commands) {
        
    }
}
