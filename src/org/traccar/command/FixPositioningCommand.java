package org.traccar.command;

import java.util.HashMap;
import java.util.Map;

public class FixPositioningCommand extends GpsCommand {
    public static final String FREQUENCY = "frequency";

    private Duration data;

    @Override
    public Map<String, Object> getReplacements() {
        Map<String, Object> replacements = new HashMap<String, Object>();
        replacements.put(FREQUENCY, data);
        return replacements;
    }

    public Duration getData() {
        return data;
    }

    public void setData(Duration data) {
        this.data = data;
    }
}
