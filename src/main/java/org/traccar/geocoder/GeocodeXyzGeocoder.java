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

public class GeocodeXyzGeocoder extends JsonGeocoder {

    private static String formatUrl(String key) {
        String url = "https://geocode.xyz/%f,%f?geoit=JSON";
        if (key != null) {
            url += "&key=" + key;
        }
        return url;
    }

    public GeocodeXyzGeocoder(Client client, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        Address address = new Address();

        if (json.containsKey("stnumber")) {
            address.setHouse(json.getString("stnumber"));
        }
        if (json.containsKey("staddress")) {
            address.setStreet(json.getString("staddress"));
        }
        if (json.containsKey("city")) {
            address.setSettlement(json.getString("city"));
        }
        if (json.containsKey("region")) {
            address.setState(json.getString("region"));
        }
        if (json.containsKey("prov")) {
            address.setCountry(json.getString("prov"));
        }
        if (json.containsKey("postal")) {
            address.setPostcode(json.getString("postal"));
        }

        return address;
    }

}
