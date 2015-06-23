package org.traccar.http.commands;

import java.util.HashMap;
import java.util.Map;

public class NoParameterCommand extends GpsCommand {
    @Override
    public Map<String, String> getReplacements() {
        return new HashMap<String, String>();
    }
}
