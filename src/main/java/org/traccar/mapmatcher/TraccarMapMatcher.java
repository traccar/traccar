/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.mapmatcher;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.InvocationCallback;

public class TraccarMapMatcher implements MapMatcher {

    private final Client client;
    private final String url;
    private final String key;

    public TraccarMapMatcher(Client client, String url, String key) {
        this.client = client;
        this.url = url != null ? url : "https://geocoder.traccar.org/snap";
        this.key = key;
    }

    @Override
    public void getPoint(
            double latitude, double longitude, MapMatcherCallback callback) {

        var request = client
                .target(url)
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("key", key)
                .request();

        request.async().get(new InvocationCallback<JsonObject>() {
            @Override
            public void completed(JsonObject json) {
                if (json.containsKey("lat") && json.containsKey("lon")) {
                    callback.onSuccess(
                            json.getJsonNumber("lat").doubleValue(),
                            json.getJsonNumber("lon").doubleValue());
                } else {
                    callback.onFailure(new IllegalStateException("Empty map matcher response"));
                }
            }

            @Override
            public void failed(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

}
