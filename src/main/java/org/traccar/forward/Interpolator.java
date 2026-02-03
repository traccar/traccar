package org.traccar.forward;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.traccar.model.Device;
import org.traccar.model.Position;

import java.io.StringWriter;
import java.util.Objects;
import java.util.Optional;

public final class Interpolator {
    private static final String LOG_TAG = Interpolator.class.getSimpleName();
    private static final VelocityEngine ENGINE = createEngine();

    private Interpolator() {
    }

    private static VelocityEngine createEngine() {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        ve.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, true);
        ve.init();
        return ve;
    }

    private static String resolve(String template, VelocityContext context) {
        Objects.requireNonNull(template, "Template string cannot be null");

        try (StringWriter writer = new StringWriter()) {
            ENGINE.evaluate(context, writer, LOG_TAG, template);
            return writer.toString();

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid forward template: check syntax and supported placeholders", e);
        }
    }

    private static VelocityContext baseContext(Device device) {
        VelocityContext context = new VelocityContext();
        context.put("uniqueId", device.getUniqueId());
        return context;
    }

    public static String resolve(String template, PositionData data) {
        VelocityContext context = baseContext(data.getDevice());
        context.put("protocol", data.getPosition().getProtocol());
        return resolve(template, context);
    }

    public static String resolve(String template, EventData data) {
        VelocityContext context = baseContext(data.getDevice());
        context.put("eventType", data.getEvent().getType());
        String protocol = Optional.ofNullable(data.getPosition())
                .map(Position::getProtocol)
                .orElse("");
        context.put("protocol", protocol);
        return resolve(template, context);
    }
}
