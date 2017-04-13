/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import org.traccar.Context;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.WifiAccessPoint;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

public class UnwiredGeolocationProvider implements GeolocationProvider {

    private String url;
    private String key;

    private ObjectMapper objectMapper;

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

    public UnwiredGeolocationProvider(String url, String key) {
        this.url = url;
        this.key = key;

        objectMapper = new ObjectMapper();
        objectMapper.addMixIn(Network.class, NetworkMixIn.class);
        objectMapper.addMixIn(CellTower.class, CellTowerMixIn.class);
        objectMapper.addMixIn(WifiAccessPoint.class, WifiAccessPointMixIn.class);
    }

    @Override
    public void getLocation(Network network, final LocationProviderCallback callback) {
        try {
            ObjectNode json = objectMapper.valueToTree(network);
            json.put("token", key);
            String request = objectMapper.writeValueAsString(json);
            Context.getAsyncHttpClient().preparePost(url)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(request.length()))
                    .setBody(request).execute(new AsyncCompletionHandler() {
                @Override
                public Object onCompleted(Response response) throws Exception {
                    try (JsonReader reader = Json.createReader(response.getResponseBodyAsStream())) {
                        JsonObject json = reader.readObject();
                        if (json.getString("status").equals("error")) {
                            callback.onFailure(
                                    new GeolocationException(json.getString("message")));
                        } else {
                            callback.onSuccess(
                                    json.getJsonNumber("lat").doubleValue(),
                                    json.getJsonNumber("lon").doubleValue(),
                                    json.getJsonNumber("accuracy").doubleValue());
                        }
                    }
                    return null;
                }

                @Override
                public void onThrowable(Throwable t) {
                    callback.onFailure(t);
                }
            });
        } catch (JsonProcessingException e) {
            callback.onFailure(e);
        }
    }

}
