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


public class BaiduGeocoder extends JsonGeocoder {

    private static String formatUrl(String key, String language) {
        String url = "https://api.map.baidu.com/reverse_geocoding/v3/"
                + "?output=json&location=%f,%f&coordtype=gcj02ll&extensions_poi=1";
        if (key != null) {
            url += "&ak=" + key;
        }
        if (language != null) {
            url += "&language=" + language + "&language_auto=1";
        }
        return url;
    }

    public BaiduGeocoder(Client client, String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key, language), cacheSize, addressFormat);
    }

    public Address parseAddress(JsonObject json) {
        JsonObject result = json.getJsonObject("result");
        if (result != null) {
            Address address = new Address();
            if (result.containsKey("formatted_address")) {
                address.setFormattedAddress(result.getString("formatted_address"));
            }

            if (result.containsKey("pois")) {
                JsonArray pois = result.getJsonArray("pois");
                if (!pois.isEmpty()) {
                    JsonObject poi = pois.getJsonObject(0);
                    if (poi.containsKey("name")) {
                        address.setHouse(poi.getString("name"));
                    }
                }
            }

            JsonObject addressComponent = result.getJsonObject("addressComponent");
            if (addressComponent != null) {
                if (addressComponent.containsKey("street")) {
                    address.setStreet(addressComponent.getString("street"));
                }
                if (addressComponent.containsKey("town")) {
                    address.setSuburb(addressComponent.getString("town"));
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
                if (addressComponent.containsKey("country")) {
                    address.setCountry(addressComponent.getString("country"));
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
