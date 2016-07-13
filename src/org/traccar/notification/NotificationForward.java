package org.traccar.notification;

import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.web.JsonConverter;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public final class NotificationForward {

    private NotificationForward() {
    }

    private static final String USER_AGENT = "Traccar Server";

    private static final String KEY_POSITION = "position";
    private static final String KEY_EVENT = "event";
    private static final String KEY_GEOFENCE = "geofence";
    private static final String KEY_DEVICE = "device";

    public static void forwardEvent(Event event, Position position) {
        String url = Context.getConfig().getString("event.forward.url", "http://localhost/");
        String header = Context.getConfig().getString("event.forward.header", "");

        BoundRequestBuilder requestBuilder = Context.getAsyncHttpClient().preparePost(url);

        requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
        requestBuilder.addHeader("User-Agent", USER_AGENT);
        if (!header.equals("")) {
            String[] headerLines = header.split("\\r?\\n");
            for (String headerLine: headerLines) {
                String[] splitedLine = headerLine.split(":", 2);
                if (splitedLine.length == 2) {
                    requestBuilder.addHeader(splitedLine[0].trim(), splitedLine[1].trim());
                }
            }
        }

        requestBuilder.setBody(preparePayload(event, position));
        requestBuilder.execute();
    }

    private static byte[] preparePayload(Event event, Position position) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add(KEY_EVENT, JsonConverter.objectToJson(event));
        if (position != null) {
            json.add(KEY_POSITION, JsonConverter.objectToJson(position));
        }
        if (event.getDeviceId() != 0) {
            Device device = Context.getIdentityManager().getDeviceById(event.getDeviceId());
            if (device != null) {
                json.add(KEY_DEVICE, JsonConverter.objectToJson(device));
            }
        }
        if (event.getGeofenceId() != 0) {
            Geofence geofence = Context.getGeofenceManager().getGeofence(event.getGeofenceId());
            if (geofence != null) {
                json.add(KEY_GEOFENCE, JsonConverter.objectToJson(geofence));
            }
        }
        return json.build().toString().getBytes(StandardCharsets.UTF_8);
    }
}
