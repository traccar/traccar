/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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

import org.traccar.model.CellTower;
import org.traccar.model.Network;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.InvocationCallback;

public class OpenCellIdGeolocationProvider implements GeolocationProvider {

    private final Client client;
    private final String url;

    public OpenCellIdGeolocationProvider(Client client, String url, String key) {
        this.client = client;
        if (url == null) {
            url = "http://opencellid.org/cell/get";
        }
        this.url = url + "?format=json&mcc=%d&mnc=%d&lac=%d&cellid=%d&key=" + key;
    }

    @Override
    public void getLocation(Network network, final LocationProviderCallback callback) {
        if (network.getCellTowers() != null && !network.getCellTowers().isEmpty()) {

            CellTower cellTower = network.getCellTowers().iterator().next();
            String request = String.format(url, cellTower.getMobileCountryCode(), cellTower.getMobileNetworkCode(),
                    cellTower.getLocationAreaCode(), cellTower.getCellId());

            client.target(request).request().async().get(new InvocationCallback<JsonObject>() {
                @Override
                public void completed(JsonObject json) {
                    if (json.containsKey("lat") && json.containsKey("lon")) {
                        callback.onSuccess(
                                json.getJsonNumber("lat").doubleValue(),
                                json.getJsonNumber("lon").doubleValue(), 0);
                    } else {
                        if (json.containsKey("error")) {
                            String errorMessage = json.getString("error");
                            if (json.containsKey("code")) {
                                errorMessage += " (" + json.getInt("code") + ")";
                            }
                            callback.onFailure(new GeolocationException(errorMessage));
                        } else {
                            callback.onFailure(new GeolocationException("Coordinates are missing"));
                        }
                    }
                }

                @Override
                public void failed(Throwable throwable) {
                    callback.onFailure(throwable);
                }
            });

        } else {
            callback.onFailure(new GeolocationException("No network information"));
        }
    }

}
