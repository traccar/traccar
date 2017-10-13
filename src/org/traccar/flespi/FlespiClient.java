package org.traccar.flespi;


import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonNumber;
import javax.json.JsonReader;
import javax.json.JsonArray;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FlespiClient {
    private final String url;
    private final String token;
    private final String channel_id;
    private final ScheduledExecutorService pullChannelExecutor = Executors.newScheduledThreadPool(5);
    private ScheduledFuture<?> pullTask;
    private Integer pullDelay = 5;
    private int nextKey;

    public FlespiClient(String url, String token, String channelId) {
        this.channel_id = channelId;
        this.url = url + "/gw/channels/" + channelId + "/messages?data={\"limit_count\":1000000,"
                + "\"limit_size\":100000000,\"delete\":true,\"timeout\":25,\"curr_key\":%d}";
        this.token = "FlespiToken " + token;

        schedulePull();
    }

    private void schedulePull() {
        pullTask = pullChannelExecutor.scheduleAtFixedRate(new ChannelPullTask(this), 1, pullDelay, TimeUnit.SECONDS);
    }

    public void stopPullTask() {
        if (pullTask != null) {
            pullTask.cancel(false);
        }
    }

    protected synchronized void channelPull() {
        Context.getAsyncHttpClient().prepareGet(String.format(this.url, nextKey))
                                    .addHeader("Authorization", this.token)
                                    .execute(new AsyncCompletionHandler() {
            @Override
            public Object onCompleted(Response response) throws Exception {
                try (JsonReader reader = Json.createReader(response.getResponseBodyAsStream())) {
                    JsonObject object = reader.readObject();
                    JsonArray result = object.getJsonArray("result");
                    nextKey = object.getInt("next_key", nextKey);
                    Log.debug(String.format("channelPull next_key=%d msgs_count=%d", nextKey, result.size()));
                    for (int i = 0; i < result.size(); i++) {
                        Position position = decodePosition(result.getJsonObject(i));
                        if (position != null && position.getLatitude() != 0 && position.getLongitude() != 0) {
                            Context.getConnectionManager().updateDevice(position.getDeviceId(),
                                    Device.STATUS_ONLINE, new Date());
                            Context.getDeviceManager().updateLatestPosition(position);
                        }
                    }
                    JsonArray errors = object.getJsonArray("errors");
                    if (errors != null) {
                        for (int i = 0; i < errors.size(); i++) {
                            JsonObject error = errors.getJsonObject(i);
                            Log.warning("Error in flespi channel: " + error.toString());
                        }
                        if (result == null || result.size() == 0) {
                            stopPullTask();
                        }
                    }
                }
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private Position decodePosition(JsonObject msg) {
        Device device = null;
        try {
            device = Context.getIdentityManager().getByUniqueId(msg.getString("ident"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (device == null) {
            return null;
        }
        Position position = new Position();
        position.setDeviceId(device.getId());


        position.setProtocol("flespi");

        position.setTime(new Date((long) msg.getJsonNumber("timestamp").doubleValue() * 1000));
        JsonNumber lat = msg.getJsonNumber("position.latitude");
        JsonNumber lon = msg.getJsonNumber("position.longitude");
        position.setLatitude((lat != null && lon != null) ? lat.doubleValue() : 0);
        position.setLongitude((lat != null && lon != null) ? lon.doubleValue() : 0);

        JsonNumber speed = msg.getJsonNumber("position.speed");
        position.setSpeed(speed != null ? speed.doubleValue() : 0);

        JsonNumber course = msg.getJsonNumber("position.direction");
        position.setCourse(course != null ? course.doubleValue() : 0);

        JsonNumber altitude = msg.getJsonNumber("position.altitude");
        position.setAltitude(altitude != null ? altitude.doubleValue() : 0);

        int satellites = msg.getInt("position.satellites", 0);
        position.setValid(lat != null && lon != null && satellites >= 3);
        position.set(Position.KEY_SATELLITES, satellites);

        return position;
    }
}
