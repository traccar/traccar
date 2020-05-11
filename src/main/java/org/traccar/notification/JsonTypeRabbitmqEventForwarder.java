package org.traccar.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.rabbitmq.RabbitmqManager;

import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.util.Set;

public class JsonTypeRabbitmqEventForwarder extends RabbitmqEventForwarder {

    @Override
    protected void forwardToRabbitmq(Event event, Position position, Set<Long> users, RabbitmqManager rabbitmqManager) {
        try {
            rabbitmqManager.sendToEventsQueue(new ObjectMapper().writeValueAsString(preparePayload(event, position, users)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
