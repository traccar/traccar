package org.traccar.command;

import org.traccar.database.ActiveDevice;

import java.util.HashMap;
import java.util.Map;

public class StringCommandTemplate<T extends GpsCommand> implements CommandTemplate<T> {

    private String messageTemplate;
    private Map<Class<?>, CommandValueConversion> converters = new HashMap<Class<?>, CommandValueConversion>();

    public StringCommandTemplate(String template, Object... replacements) {
        this.messageTemplate = String.format(template, replacements);
    }

    @Override
    public Object applyTo(ActiveDevice activeDevice, T command) {
        String currentMessage = messageTemplate;
        currentMessage = this.replace(currentMessage, GpsCommand.UNIQUE_ID, activeDevice.getUniqueId());

        Map<String, Object> replacements = command.getReplacements();

        for (Map.Entry<String, Object> entry : replacements.entrySet()) {
            currentMessage = this.replace(currentMessage, entry.getKey(), entry.getValue());
        }

        return currentMessage;
    }

    public CommandTemplate addConverter(Class<?> type, CommandValueConversion converter) {
        converters.put(type, converter);
        return this;
    }

    protected CommandValueConversion getConverter(Class<?> type) {
        return converters.containsKey(type) ? converters.get(type) : idConverter();
    }

    private CommandValueConversion idConverter() {
        return new CommandValueConversion() {
            @Override
            public String convert(Object value) {
                return value.toString();
            }
        };
    }

    private String replace(String currentMessage, String key, Object value) {
        String replacementValue = getConverter(value.getClass()).convert(value);
        return currentMessage.replace("[" + key + "]", replacementValue);
    }

}
