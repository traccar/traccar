package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;
import org.traccar.http.commands.FixPositioningCommand;

import java.util.HashMap;
import java.util.Map;

public class Gps103Protocol extends BaseProtocol {

    public Gps103Protocol() {
        super("gps103");
    }

    @Override
    protected void loadCommandsDefinitions(Map<CommandType, String> commands) {
        commands.put(CommandType.STOP_POSITIONING, "**,imei:[uniqueId],A");
        commands.put(CommandType.FIX_POSITIONING, String.format("**,imei:[uniqueId],C,[%s]", FixPositioningCommand.FREQUENCY));
        commands.put(CommandType.RESUME_ENGINE, "**,imei:[uniqueId],J");
        commands.put(CommandType.STOP_ENGINE, "**,imei:[uniqueId],K");
    }
}
