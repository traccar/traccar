package org.traccar;

import org.traccar.database.ActiveDevice;
import org.traccar.command.GpsCommand;

import java.util.List;

public interface Protocol {

    public String getName();

    void sendCommand(ActiveDevice activeDevice, GpsCommand command);

    void initTrackerServers(List<TrackerServer> serverList);
}
