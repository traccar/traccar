package org.traccar.forward;

import org.traccar.model.Position;

import java.util.Optional;

public final class Interpolator {

    private Interpolator() {
    }

    private static String resolve(String template, String uniqueId, String protocol, String eventType) {
        return template
                .replace("{uniqueId}", uniqueId)
                .replace("{protocol}", protocol)
                .replace("{eventType}", eventType);
    }

    public static String resolve(String template, PositionData data) {
        String uniqueId = data.getDevice().getUniqueId();
        String protocol = Optional.ofNullable(data.getPosition())
                .map(Position::getProtocol)
                .orElse("");
        String eventType = "";

        return resolve(template, uniqueId, protocol, eventType);
    }

    public static String resolve(String template, EventData data) {
        String uniqueId = data.getDevice().getUniqueId();
        String protocol = Optional.ofNullable(data.getPosition())
                .map(Position::getProtocol)
                .orElse("");
        String eventType = data.getEvent().getType();

        return resolve(template, uniqueId, protocol, eventType);
    }
}
