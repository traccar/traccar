package org.traccar.command;

import java.util.Map;

public abstract class GpsCommand {
    public static final String UNIQUE_ID = "uniqueId";
    private String uniqueId;
    private CommandType type;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    public abstract Map<String, Object> getReplacements();
}
