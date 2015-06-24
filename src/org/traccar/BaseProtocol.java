package org.traccar;

import org.traccar.database.ActiveDevice;
import org.traccar.http.commands.CommandType;
import org.traccar.http.commands.GpsCommand;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseProtocol implements Protocol {

    private final String name;
    private Map<CommandType, String> commands = new HashMap<CommandType, String>();

    public BaseProtocol(String name) {
        this.name = name;
        this.loadCommandsDefinitions(commands);
    }

    public String getName() {
        return name;
    }

    @Override
    public void sendCommand(ActiveDevice activeDevice, GpsCommand command) {

    }

    protected abstract void loadCommandsDefinitions(Map<CommandType, String> commands);

}
