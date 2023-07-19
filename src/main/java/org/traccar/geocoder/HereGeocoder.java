/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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

import javax.json.JsonObject;
import javax.ws.rs.client.Client;

public class HereGeocoder extends JsonGeocoder {

    private static String formatUrl(String url, String id, String key, String language) {
        if (url == null) {
            url = "https://reverse.geocoder.ls.hereapi.com/6.2/reversegeocode.json";
        }
        url += "?mode=retrieveAddresses&maxresults=1";
        url += "&prox=%f,%f,0";
        url += "&app_id=" + id;
        url += "&app_code=" + key;
        url += "&apiKey=" + key;
        if (language != null) {
            url += "&language=" + language;
        }
        return url;
    }

    public HereGeocoder(
            Client client, String url, String id, String key, String language,
            int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(url, id, key, language), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonObject result = json
                .getJsonObject("Response")
                .getJsonArray("View")
                .getJsonObject(0)
                .getJsonArray("Result")
                .getJsonObject(0)
                .getJsonObject("Location")
                .getJsonObject("Address");

        if (result != null) {
            Address address = new Address();

            if (result.containsKey("Label")) {
                address.setFormattedAddress(result.getString("Label"));
            }

            if (result.containsKey("HouseNumber")) {
                address.setHouse(result.getString("HouseNumber"));
            }
            if (result.containsKey("Street")) {
                address.setStreet(result.getString("Street"));
            }
            if (result.containsKey("City")) {
                address.setSettlement(result.getString("City"));
            }
            if (result.containsKey("District")) {
                address.setDistrict(result.getString("District"));
            }
            if (result.containsKey("State")) {
                address.setState(result.getString("State"));
            }
            if (result.containsKey("Country")) {
                address.setCountry(result.getString("Country").toUpperCase());
            }
            if (result.containsKey("PostalCode")) {
                address.setPostcode(result.getString("PostalCode"));
            }

            return address;
        }

        return null;
    }

}
