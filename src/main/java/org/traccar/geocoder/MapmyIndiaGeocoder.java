/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;

public class MapmyIndiaGeocoder extends JsonGeocoder {

    public MapmyIndiaGeocoder(Client client, String url, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, url + "/" + key + "/rev_geocode?lat=%f&lng=%f", cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonArray results = json.getJsonArray("results");

        if (!results.isEmpty()) {
            Address address = new Address();

            JsonObject result = (JsonObject) results.get(0);

            if (result.containsKey("formatted_address")) {
                address.setFormattedAddress(result.getString("formatted_address"));
            }

            if (result.containsKey("house_number") && !result.getString("house_number").isEmpty()) {
                address.setHouse(result.getString("house_number"));
            } else if (result.containsKey("house_name") && !result.getString("house_name").isEmpty()) {
                address.setHouse(result.getString("house_name"));
            }

            if (result.containsKey("street")) {
                address.setStreet(result.getString("street"));
            }

            if (result.containsKey("locality") && !result.getString("locality").isEmpty()) {
                address.setSuburb(result.getString("locality"));
            } else if (result.containsKey("sublocality") && !result.getString("sublocality").isEmpty()) {
                address.setSuburb(result.getString("sublocality"));
            } else if (result.containsKey("subsublocality") && !result.getString("subsublocality").isEmpty()) {
                address.setSuburb(result.getString("subsublocality"));
            }

            if (result.containsKey("city") && !result.getString("city").isEmpty()) {
                address.setSettlement(result.getString("city"));
            } else if (result.containsKey("village") && !result.getString("village").isEmpty()) {
                address.setSettlement(result.getString("village"));
            }

            if (result.containsKey("district")) {
                address.setDistrict(result.getString("district"));
            } else if (result.containsKey("subDistrict")) {
                address.setDistrict(result.getString("subDistrict"));
            }

            if (result.containsKey("state")) {
                address.setState(result.getString("state"));
            }

            if (result.containsKey("pincode")) {
                address.setPostcode(result.getString("pincode"));
            }

            return address;
        }
        return null;
    }
}
