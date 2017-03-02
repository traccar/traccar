package org.traccar;

import org.traccar.database.ActiveDevice;
import org.traccar.model.Command;

import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

import java.util.Collection;
import java.util.List;

public interface Protocol {

    String getName();

    Collection<String> getSupportedCommands();

    void sendCommand(ActiveDevice activeDevice, Command command);

    void initTrackerServers(List<TrackerServer> serverList);

    Collection<String> getSupportedSmsCommands();

    void sendSmsCommand(String phone, Command command) throws RecoverablePduException, UnrecoverablePduException,
            SmppTimeoutException, SmppChannelException, InterruptedException;

}
