/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

public class GeocodeFarmGeocoder extends JsonGeocoder {

    private static final String URL = "https://www.geocode.farm/v3/json/reverse/";

    public GeocodeFarmGeocoder(int cacheSize) {
        super(URL + "?lat=%f&lon=%f&country=us&lang=en&count=1", cacheSize);
    }

    public GeocodeFarmGeocoder(String key, int cacheSize) {
        super(URL + "?lat=%f&lon=%f&country=us&lang=en&count=1&key=" + key, cacheSize);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        Address address = new Address();

        JsonObject result = json
                .getJsonObject("geocoding_results")
                .getJsonArray("RESULTS")
                .getJsonObject(0)
                .getJsonObject("ADDRESS");

        if (result.containsKey("street_number")) {
            address.setStreet(result.getString("street_number"));
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

        return address;
    }

}
