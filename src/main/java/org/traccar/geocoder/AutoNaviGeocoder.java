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
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import org.traccar.helper.CoordinateUtil;


public class AutoNaviGeocoder extends JsonGeocoder {

    private static String formatUrl(String key) {
        String url = "https://restapi.amap.com/v3/geocode/regeo?output=json&location=%2$f,%1$f&extensions=all";
        if (key != null) {
            url += "&key=" + key;
        }
        return url;
    }

    public AutoNaviGeocoder(Client client, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key), cacheSize, addressFormat);
    }

    public Address parseAddress(JsonObject json) {
        JsonObject regeocode = json.getJsonObject("regeocode");
        if (regeocode != null) {
            Address address = new Address();

            address.setFormattedAddress(getJsonNode(regeocode, "formatted_address"));

            JsonObject addressComponent = regeocode.getJsonObject("addressComponent");
            if (addressComponent != null) {
                JsonObject building = addressComponent.getJsonObject("building");
                String houseName = getJsonNode(building, "name");
                if (houseName == null) {
                    JsonArray aois = regeocode.getJsonArray("aois");
                    if (!aois.isEmpty()) {
                        JsonObject aoi = aois.getJsonObject(0);
                        houseName = getJsonNode(aoi, "name");
                    }
                }
                if (houseName == null) {
                    JsonArray pois = regeocode.getJsonArray("pois");
                    if (!pois.isEmpty()) {
                        JsonObject poi = pois.getJsonObject(0);
                        houseName = getJsonNode(poi, "name");
                    }
                }
                address.setHouse(houseName);

                JsonObject street = addressComponent.getJsonObject("streetNumber");
                address.setStreet(getJsonNode(street, "street"));

                address.setSuburb(getJsonNode(addressComponent, "township"));
                address.setDistrict(getJsonNode(addressComponent, "district"));
                address.setSettlement(getJsonNode(addressComponent, "city"));
                address.setState(getJsonNode(addressComponent, "province"));
                address.setCountry(getJsonNode(addressComponent, "country"));
            }
            return address;
        }
        return null;
    }

    private String getJsonNode(JsonObject json, String key) {
        if (json != null && json.containsKey(key)) {
            JsonValue value = json.get(key);
            if (value.getValueType() == JsonValue.ValueType.STRING) {
                return ((JsonString) value).getString();
            }
        }
        return null;
    }

    protected String parseError(JsonObject json) {
        return json.getString("info");
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
