/*
 * Copyright 2024 - 2024 容均致 (harryrong@rushanio.com)
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
package org.traccar.geocoder;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import org.traccar.helper.CoordinateUtil;


public class TencentGeocoder extends JsonGeocoder {

    private static String formatUrl(String key) {
        String url = "https://apis.map.qq.com/ws/geocoder/v1/?location=%f,%f&get_poi=1&output=json";
        if (key != null) {
            url += "&key=" + key;
        }
        return url;
    }

    public TencentGeocoder(Client client, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key), cacheSize, addressFormat);
    }

    public Address parseAddress(JsonObject json) {
        JsonObject result = json.getJsonObject("result");
        if (result != null) {
            Address address = new Address();
            if (result.containsKey("address")) {
                address.setFormattedAddress(result.getString("address"));
            }

            if (result.containsKey("pois")) {
                JsonArray pois = result.getJsonArray("pois");
                if (!pois.isEmpty()) {
                    JsonObject poi = pois.getJsonObject(0);
                    if (poi.containsKey("title")) {
                        address.setHouse(poi.getString("title"));
                    }
                }
            }

            JsonObject addressComponent = result.getJsonObject("address_component");
            if (addressComponent != null) {
                if (addressComponent.containsKey("street")) {
                    address.setStreet(addressComponent.getString("street"));
                }
                if (addressComponent.containsKey("district")) {
                    address.setDistrict(addressComponent.getString("district"));
                }
                if (addressComponent.containsKey("city")) {
                    address.setSettlement(addressComponent.getString("city"));
                }
                if (addressComponent.containsKey("province")) {
                    address.setState(addressComponent.getString("province"));
                }
                if (addressComponent.containsKey("nation")) {
                    address.setCountry(addressComponent.getString("nation"));
                }
            }

            if (result.containsKey("address_reference")) {
                JsonObject addressReference = result.getJsonObject("address_reference");
                if (addressReference.containsKey("town")) {
                    JsonObject town = addressReference.getJsonObject("town");
                    if (town.containsKey("title")) {
                        address.setSuburb(town.getString("title"));
                    }
                }
            }

            return address;
        }
        return null;
    }

    protected String parseError(JsonObject json) {
        return json.getString("message");
    }

    @Override
    public String getAddress(
            final double latitude, final double longitude, final ReverseGeocoderCallback callback) {
        if (CoordinateUtil.outOfChina(latitude, longitude)) {
            return null;
        }
        CoordinateUtil.Coordinate coordinate = CoordinateUtil.wgs84ToGcj02(latitude, longitude);

        return super.getAddress(coordinate.getLatitude(), coordinate.getLongitude(), callback);
    }
}
