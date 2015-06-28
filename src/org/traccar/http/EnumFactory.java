package org.traccar.http;

import org.traccar.model.Factory;

import javax.json.JsonObject;

public class EnumFactory<T extends Enum<T> & Factory> {
    private Class<T> commandTypeClass;
    private String jsonKey;

    public EnumFactory(Class<T> commandTypeClass, String type) {
        this.commandTypeClass = commandTypeClass;
        jsonKey = type;
    }

    public <K> K create(JsonObject json) {
        Factory factory = Enum.valueOf(commandTypeClass, json.getString(jsonKey));
        return (K) factory.create();
    }
}
