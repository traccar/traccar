package org.traccar;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.traccar.database.ActiveDevice;
import org.traccar.model.Command;

public interface Protocol {

    String getName();

    void sendCommand(ActiveDevice activeDevice, Command command);
    
     public Set<String> getSupportedCommands();

    void initTrackerServers(List<TrackerServer> serverList);

}
