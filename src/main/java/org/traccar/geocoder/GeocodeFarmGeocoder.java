/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
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

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;

public class GeocodeFarmGeocoder extends JsonGeocoder {

    private static String formatUrl(String key, String language) {
        String url = "https://api.geocode.farm/reverse/?lat=%f&lon=%f";
        if (key != null) {
            url += "&key=" + key;
        }
        if (language != null) {
            url += "&lang=" + language;
        }
        return url;
    }

    public GeocodeFarmGeocoder(
            Client client, String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key, language), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        Address address = new Address();

        JsonObject result = json.getJsonObject("RESULTS").getJsonObject("result").getJsonObject("0");

        if (result.containsKey("formatted_address")) {
            address.setFormattedAddress(result.getString("formatted_address"));
        }
        if (result.containsKey("house_number")) {
            address.setHouse(result.getString("house_number"));
        }
        if (result.containsKey("street_name")) {
            address.setStreet(result.getString("street_name"));
        }
        if (result.containsKey("locality")) {
            address.setSettlement(result.getString("locality"));
        }
        if (result.containsKey("admin_1")) {
            address.setState(result.getString("admin_1"));
        }
        if (result.containsKey("country")) {
            address.setCountry(result.getString("country"));
        }
        if (result.containsKey("postal_code")) {
            address.setPostcode(result.getString("postal_code"));
        }

        return address;
    }

}
