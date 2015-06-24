package org.traccar.http.commands;

import java.util.HashMap;
import java.util.Map;

public class FixPositioningCommand extends GpsCommand {
    public static final String FREQUENCY = "frequency";

    private Duration data;

    @Override
    public Map<String, String> getReplacements() {
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put(FREQUENCY, data.toCommandFormat());
        return replacements;
    }

    public Duration getData() {
        return data;
    }

    public void setData(Duration data) {
        this.data = data;
    }
}
