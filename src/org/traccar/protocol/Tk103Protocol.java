package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;

import java.util.HashMap;
import java.util.Map;

public class Tk103Protocol extends BaseProtocol {

    public Tk103Protocol() {
        super("tk103");
    }

    @Override
    protected void loadCommandsDefinitions(Map<CommandType, String> commands) {

    }
}
