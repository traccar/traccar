package org.traccar.notification;

import java.util.Set;

import org.traccar.model.Event;
import org.traccar.model.Position;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class JsonTypeEventForwarder extends EventForwarder {

    @Override
    protected String getContentType() {
        return "application/json; charset=utf-8";
    }

    @Override
    protected void setContent(Event event, Position position, Set<Long> users, BoundRequestBuilder requestBuilder) {
        requestBuilder.setBody(prepareJsonPayload(event, position, users));
    }

}
