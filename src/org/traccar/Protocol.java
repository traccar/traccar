package org.traccar;

import org.traccar.model.Command;
import org.traccar.database.ActiveDevice;

import java.util.List;

public interface Protocol {

    public String getName();

    void sendCommand(ActiveDevice activeDevice, Command command);

    void initTrackerServers(List<TrackerServer> serverList);
}
