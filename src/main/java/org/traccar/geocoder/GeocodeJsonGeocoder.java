/*
 * Copyright 2014 - 2024 Anton Tananaev (anton@traccar.org)
 * Copyright 2024 - 2024 Matjaž Črnko (m.crnko@txt.i)
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

public class GeocodeJsonGeocoder extends JsonGeocoder {

    private static String formatUrl(String url, String key, String language) {
        if (url == null) {
            url = "https://photon.komoot.io/reverse";
        }
        url += "?lat=%f&lon=%f";
        if (key != null) {
            url += "&key=" + key;
        }
        if (language != null) {
            url += "&lang=" + language;
        }
        return url;
    }

    public GeocodeJsonGeocoder(
            Client client, String url, String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(url, key, language), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonArray features = json.getJsonArray("features");
        if (!features.isEmpty()) {
            Address address = new Address();
            JsonObject properties = features.getJsonObject(0).getJsonObject("properties");

            if (properties.containsKey("label")) {
                address.setFormattedAddress(properties.getString("label"));
            }
            if (properties.containsKey("housenumber")) {
                address.setHouse(properties.getString("housenumber"));
            }
            if (properties.containsKey("street")) {
                address.setStreet(properties.getString("street"));
            }
            if (properties.containsKey("city")) {
                address.setSettlement(properties.getString("city"));
            }
            if (properties.containsKey("district")) {
                address.setDistrict(properties.getString("district"));
            }
            if (properties.containsKey("state")) {
                address.setState(properties.getString("state"));
            }
            if (properties.containsKey("countrycode")) {
                address.setCountry(properties.getString("countrycode").toUpperCase());
            }
            if (properties.containsKey("postcode")) {
                address.setPostcode(properties.getString("postcode"));
            }

            return address;
        }
        return null;
    }

}
