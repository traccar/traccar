package org.traccar.notification;

import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class FormTypeEventForwarder extends EventForwarder {

    private final String payloadParamName;
    private final String additionalParams;

    public FormTypeEventForwarder() {
        payloadParamName = Context.getConfig().getString("event.forward.paramMode.payloadParamName", "payload");
        additionalParams = Context.getConfig().getString("event.forward.paramMode.additionalParams", "");
    }

    @Override
    protected String getContentType() {
        return "application/x-www-form-urlencoded; charset=UTF-8";
    }

    @Override
    protected void setContent(Event event, Position position, BoundRequestBuilder requestBuilder) {
        if (!additionalParams.equals("")) {
            requestBuilder.setFormParams(splitParams(additionalParams, "="));
        }

        requestBuilder.addFormParam(payloadParamName, prepareJsonPayload(event, position));
    }

}
