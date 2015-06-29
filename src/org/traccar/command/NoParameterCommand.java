package org.traccar.command;

import java.util.HashMap;
import java.util.Map;

public class NoParameterCommand extends GpsCommand {
    @Override
    public Map<String, Object> getReplacements() {
        return new HashMap<String, Object>();
    }
}
