package org.traccar.forward;

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
        String protocol = data.getPosition().getProtocol();
        String eventType = "";

        return resolve(template, uniqueId, protocol, eventType);
    }

    public static String resolve(String template, EventData data) {
        String uniqueId = data.getDevice().getUniqueId() ;
        String protocol = data.getPosition() != null ? data.getPosition().getProtocol() : "";
        String eventType = data.getEvent().getType();

        return resolve(template, uniqueId, protocol, eventType);
    }
}
