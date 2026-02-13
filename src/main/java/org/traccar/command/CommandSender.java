package org.traccar.command;

import org.traccar.model.Command;
import org.traccar.model.Device;

import java.util.Collection;

public interface CommandSender {
    Collection<String> getSupportedCommands();
    void sendCommand(Device device, Command command) throws Exception;
}
