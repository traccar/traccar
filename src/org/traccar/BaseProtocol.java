package org.traccar;

import org.traccar.database.ActiveDevice;
import org.traccar.http.commands.CommandType;
import org.traccar.protocol.commands.CommandTemplate;
import org.traccar.http.commands.GpsCommand;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseProtocol implements Protocol {

    private final String name;
    private Map<CommandType, CommandTemplate> commandTemplates = new HashMap<CommandType, CommandTemplate>();

    public BaseProtocol(String name) {
        this.name = name;
        this.loadCommandsTemplates(commandTemplates);
    }

    public String getName() {
        return name;
    }

    @Override
    public void sendCommand(ActiveDevice activeDevice, GpsCommand command) {
        CommandTemplate commandMessage = commandTemplates.get(command.getType());

        if (commandMessage == null) {
            throw new RuntimeException("The command " + command + " is not yet supported in protocol " + this.getName());
        }

        Object response = commandMessage.applyTo(activeDevice, command);

        activeDevice.write(response);
    }

    protected abstract void loadCommandsTemplates(Map<CommandType, CommandTemplate> templates);

}
