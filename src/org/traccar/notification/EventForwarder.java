/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.notification;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Position;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;

public final class EventForwarder {

    private String url;
    private String header;
    private boolean payloadAsParamMode;
    private String payloadParamName;
    private String additionalParams;
    private boolean prettyPrinted;

    public EventForwarder() {
        url = Context.getConfig().getString("event.forward.url", "http://localhost/");
        header = Context.getConfig().getString("event.forward.header", "");
        payloadAsParamMode = Context.getConfig().getBoolean("event.forward.payloadAsParamMode");
        payloadParamName = Context.getConfig().getString("event.forward.paramMode.payloadParamName", "payload");
        additionalParams = Context.getConfig().getString("event.forward.paramMode.additionalParams", "");
        prettyPrinted = Context.getConfig().getBoolean("event.forward.prettyPrintedPayload");
    }

    private static final String KEY_POSITION = "position";
    private static final String KEY_EVENT = "event";
    private static final String KEY_GEOFENCE = "geofence";
    private static final String KEY_DEVICE = "device";

    public void forwardEvent(Event event, Position position) {

        BoundRequestBuilder requestBuilder = Context.getAsyncHttpClient().preparePost(url);
        requestBuilder.setBodyEncoding(StandardCharsets.UTF_8.name());

        setContentTypeFromMode(requestBuilder);

        if (!header.equals("")) {
            FluentCaseInsensitiveStringsMap params = new FluentCaseInsensitiveStringsMap();
            params.putAll(splitParams(header, ":"));
            requestBuilder.setHeaders(params);
        }

        if (payloadAsParamMode) {
            setParamsPayload(event, position, requestBuilder);
        } else {
            setJsonPayload(event, position, requestBuilder);
        }

        requestBuilder.execute();
    }

    private void setJsonPayload(Event event, Position position, BoundRequestBuilder requestBuilder) {
        requestBuilder.setBody(preparePayload(event, position));
    }

    private void setParamsPayload(Event event, Position position, BoundRequestBuilder requestBuilder) {
        if (!additionalParams.equals("")) {
            requestBuilder.setFormParams(splitParams(additionalParams, "="));
        }
        requestBuilder.addFormParam(payloadParamName,
                NotificationFormatter.formatForwarderMessage(event, position));
    }

    private Map<String, List<String>> splitParams(String params, String separator) {

        String[] splitedLine;
        Map<String, List<String>> paramsMap = new HashMap<>();
        String[] paramsLines = params.split("\\r?\\n");

        for (String paramLine: paramsLines) {
            splitedLine = paramLine.split(separator, 2);
            if (splitedLine.length == 2) {
                paramsMap.put(splitedLine[0].trim(), Arrays.asList(splitedLine[1].trim()));
            }
        }
        return paramsMap;
    }

    private void setContentTypeFromMode(BoundRequestBuilder requestBuilder) {
        if (payloadAsParamMode) {
            requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        } else {
            requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
        }
    }

    private String preparePayload(Event event, Position position) {
        Map<String, Object> data = new HashMap<>();
        data.put(KEY_EVENT, event);
        if (position != null) {
            data.put(KEY_POSITION, position);
        }
        if (event.getDeviceId() != 0) {
            Device device = Context.getIdentityManager().getById(event.getDeviceId());
            if (device != null) {
                data.put(KEY_DEVICE, device);
            }
        }
        if (event.getGeofenceId() != 0) {
            Geofence geofence = (Geofence) Context.getGeofenceManager().getById(event.getGeofenceId());
            if (geofence != null) {
                data.put(KEY_GEOFENCE, geofence);
            }
        }
        try {
            return getObjectWriter().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            Log.warning(e);
            return null;
        }
    }

    private ObjectWriter getObjectWriter() {
        return prettyPrinted
            ? Context.getObjectWriterPrettyPrinter()
            : Context.getObjectMapper().writer();
    }

}
