package org.traccar.forward;

import java.util.Objects;

public final class Interpolator {

    private Interpolator() {
    }

    private static String resolve(String template, String uniqueId, String protocol, String eventType) {
        Objects.requireNonNull(template, "Template string cannot be null");

        return template
                .replace("${uniqueId}", uniqueId)
                .replace("${protocol}", protocol)
                .replace("${eventType}", eventType);
    }

    public static String resolve(String template, PositionData data) {
        Objects.requireNonNull(data, "Position data cannot be null");

        String uniqueId = data.getDevice() != null ? data.getDevice().getUniqueId() : "";
        String protocol = data.getPosition() != null ? data.getPosition().getProtocol() : "";
        String eventType = "";

        return resolve(template, uniqueId, protocol, eventType);
    }

    public static String resolve(String template, EventData data) {
        Objects.requireNonNull(data, "Event data cannot be null");

        String uniqueId = data.getDevice() != null ? data.getDevice().getUniqueId() : "";
        String protocol = data.getPosition() != null ? data.getPosition().getProtocol() : "";
        String eventType = data.getEvent() != null ? data.getEvent().getType() : "";

        return resolve(template, uniqueId, protocol, eventType);
    }
}
