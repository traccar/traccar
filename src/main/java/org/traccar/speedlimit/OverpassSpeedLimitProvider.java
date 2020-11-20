/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
package org.traccar.speedlimit;

import org.traccar.Context;
import org.traccar.helper.UnitsConverter;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.InvocationCallback;

public class OverpassSpeedLimitProvider implements SpeedLimitProvider {

    private final String url;

    public OverpassSpeedLimitProvider(String url) {
        this.url = url + "?data=[out:json];way[maxspeed](around:100.0,%f,%f);out%%20tags;";
    }

    private Double parseSpeed(String value) {
        if (value.endsWith(" mph")) {
            return UnitsConverter.knotsFromMph(Double.parseDouble(value.substring(0, value.length() - 4)));
        } else if (value.endsWith(" knots")) {
            return Double.parseDouble(value.substring(0, value.length() - 6));
        } else if (value.matches("\\d+")) {
            return UnitsConverter.knotsFromKph(Double.parseDouble(value));
        } else {
            return null;
        }
    }

    @Override
    public void getSpeedLimit(double latitude, double longitude, SpeedLimitProviderCallback callback) {
        String formattedUrl = String.format(url, latitude, longitude);
        AsyncInvoker invoker = Context.getClient().target(formattedUrl).request().async();
        invoker.get(new InvocationCallback<JsonObject>() {
            @Override
            public void completed(JsonObject json) {
                JsonArray elements = json.getJsonArray("elements");
                if (!elements.isEmpty()) {
                    Double maxSpeed = parseSpeed(
                            elements.getJsonObject(0).getJsonObject("tags").getString("maxspeed"));
                    if (maxSpeed != null) {
                        callback.onSuccess(maxSpeed);
                    } else {
                        callback.onFailure(new SpeedLimitException("Parsing failed"));
                    }
                } else {
                    callback.onFailure(new SpeedLimitException("Not found"));
                }
            }

            @Override
            public void failed(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

}
