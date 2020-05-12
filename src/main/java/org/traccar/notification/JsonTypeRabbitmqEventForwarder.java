package org.traccar.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.rabbitmq.RabbitmqManager;

import java.io.IOException;
import java.util.Set;

public class JsonTypeRabbitmqEventForwarder extends RabbitmqEventForwarder {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTypeRabbitmqEventForwarder.class);

    @Override
    protected void forwardToRabbitmq(Event event, Position position, Set<Long> users, RabbitmqManager rabbitmqManager) {
        try {
            rabbitmqManager.sendToEventsQueue(new ObjectMapper().writeValueAsString(preparePayload(event, position, users)));
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

}
