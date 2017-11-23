package org.traccar.notification;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.multipart.StringPart;

public class MultiPartEventForwarder extends EventForwarder {

    private final String payloadParamName;
    private final String additionalParams;

    public MultiPartEventForwarder() {
        payloadParamName = Context.getConfig().getString("event.forward.paramMode.payloadParamName", "payload");
        additionalParams = Context.getConfig().getString("event.forward.paramMode.additionalParams");
    }

    @Override
    protected String getContentType() {
        return "multipart/form-data";
    }

    @Override
    protected void setContent(Event event, Position position, BoundRequestBuilder requestBuilder) {

        if (additionalParams != null && !additionalParams.isEmpty()) {
            Map<String, List<String>> paramsToAdd = splitIntoKeyValues(additionalParams, "=");

            for (Entry<String, List<String>> param : paramsToAdd.entrySet()) {
                for (String singleParamValue : param.getValue()) {
                    requestBuilder.addBodyPart(new StringPart(param.getKey(), singleParamValue, null,
                            StandardCharsets.UTF_8));
                }
            }
        }
        requestBuilder.addBodyPart(new StringPart(payloadParamName,
                prepareJsonPayload(event, position), "application/json", StandardCharsets.UTF_8));
    }
}
