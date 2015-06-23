package org.traccar.protocol.commands;

import org.traccar.database.ActiveDevice;
import org.traccar.http.commands.GpsCommand;

public interface CommandTemplate<T extends GpsCommand> {
    Object applyTo(ActiveDevice activeDevice, T command);
}
