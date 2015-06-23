package org.traccar;

import org.traccar.database.ActiveDevice;
import org.traccar.http.commands.GpsCommand;

import java.util.List;

public interface Protocol {

    public String getName();

    void sendCommand(ActiveDevice activeDevice, GpsCommand command);

    void addTrackerServersTo(List<TrackerServer> serverList);
}
