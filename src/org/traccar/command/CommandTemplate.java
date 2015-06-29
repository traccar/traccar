package org.traccar.command;

import org.traccar.database.ActiveDevice;
import org.traccar.command.GpsCommand;

public interface CommandTemplate<T extends GpsCommand> {
    Object applyTo(ActiveDevice activeDevice, T command);
}
