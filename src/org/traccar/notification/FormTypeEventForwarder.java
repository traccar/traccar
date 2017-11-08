package org.traccar.notification;

import org.traccar.model.Event;
import org.traccar.model.Position;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class FormTypeEventForwarder extends EventForwarder {

    @Override
    protected String getContentType() {
        return "application/x-www-form-urlencoded; charset=UTF-8";
    }

    @Override
    protected void setContent(Event event, Position position, BoundRequestBuilder requestBuilder) {
        if (!getAdditionalParams().equals("")) {
            requestBuilder.setFormParams(splitParams(getAdditionalParams(), "="));
        }

        String payload = isUseTemplatesForPayload()
                ? NotificationFormatter.formatForwarderMessage(event, position)
                : prepareJsonPayload(event, position);

        requestBuilder.addFormParam(getPayloadParamName(), payload);
    }

}
