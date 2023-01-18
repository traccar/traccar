/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.WifiAccessPoint;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import java.util.Collection;

public class UnwiredGeolocationProvider implements GeolocationProvider {

    private final Client client;
    private final String url;
    private final String key;

    private final ObjectMapper objectMapper;

    private abstract static class NetworkMixIn {
        @JsonProperty("mcc")
        abstract Integer getHomeMobileCountryCode();
        @JsonProperty("mnc")
        abstract Integer getHomeMobileNetworkCode();
        @JsonProperty("radio")
        abstract String getRadioType();
        @JsonIgnore
        abstract String getCarrier();
        @JsonIgnore
        abstract Boolean getConsiderIp();
        @JsonProperty("cells")
        abstract Collection<CellTower> getCellTowers();
        @JsonProperty("wifi")
        abstract Collection<WifiAccessPoint> getWifiAccessPoints();
    }

    private abstract static class CellTowerMixIn {
        @JsonProperty("radio")
        abstract String getRadioType();
        @JsonProperty("mcc")
        abstract Integer getMobileCountryCode();
        @JsonProperty("mnc")
        abstract Integer getMobileNetworkCode();
        @JsonProperty("lac")
        abstract Integer getLocationAreaCode();
        @JsonProperty("cid")
        abstract Long getCellId();
    }

    private abstract static class WifiAccessPointMixIn {
        @JsonProperty("bssid")
        abstract String getMacAddress();
        @JsonProperty("signal")
        abstract Integer getSignalStrength();
    }

    public UnwiredGeolocationProvider(Client client, String url, String key) {
        this.client = client;
        this.url = url;
        this.key = key;

        objectMapper = new ObjectMapper();
        objectMapper.addMixIn(Network.class, NetworkMixIn.class);
        objectMapper.addMixIn(CellTower.class, CellTowerMixIn.class);
        objectMapper.addMixIn(WifiAccessPoint.class, WifiAccessPointMixIn.class);
    }

    @Override
    public void getLocation(Network network, final LocationProviderCallback callback) {
        ObjectNode json = objectMapper.valueToTree(network);
        json.put("token", key);

        client.target(url).request().async().post(Entity.json(json), new InvocationCallback<JsonObject>() {
            @Override
            public void completed(JsonObject json) {
                if (json.getString("status").equals("error")) {
                    callback.onFailure(new GeolocationException(json.getString("message")));
                } else {
                    callback.onSuccess(
                            json.getJsonNumber("lat").doubleValue(),
                            json.getJsonNumber("lon").doubleValue(),
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
