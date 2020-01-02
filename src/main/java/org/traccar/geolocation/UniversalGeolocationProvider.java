/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar.geolocation;

import org.traccar.Context;
import org.traccar.model.Network;

import javax.json.JsonObject;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;

public class UniversalGeolocationProvider implements GeolocationProvider {

    private String url;

    public UniversalGeolocationProvider(String url, String key) {
        this.url = url + "?key=" + key;
    }

    @Override
    public void getLocation(Network network, final LocationProviderCallback callback) {
        AsyncInvoker invoker = Context.getClient().target(url).request().async();
        invoker.post(Entity.json(network), new InvocationCallback<JsonObject>() {
            @Override
            public void completed(JsonObject json) {
                if (json.containsKey("error")) {
                    callback.onFailure(new GeolocationException(json.getJsonObject("error").getString("message")));
                } else {
                    JsonObject location = json.getJsonObject("location");
                    callback.onSuccess(
                            location.getJsonNumber("lat").doubleValue(),
                            location.getJsonNumber("lng").doubleValue(),
                            json.getJsonNumber("accuracy").doubleValue());
                }
            }

            @Override
            public void failed(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

}
