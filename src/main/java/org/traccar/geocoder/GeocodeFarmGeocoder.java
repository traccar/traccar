/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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

public class GeocodeFarmGeocoder extends JsonGeocoder {

    private static String formatUrl(String key, String language) {
        String url = "https://www.geocode.farm/v3/json/reverse/";
        url += "?lat=%f&lon=%f&country=us&count=1";
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

        JsonObject result = json
                .getJsonObject("geocoding_results")
                .getJsonArray("RESULTS")
                .getJsonObject(0);

        JsonObject resultAddress = result.getJsonObject("ADDRESS");

        if (result.containsKey("formatted_address")) {
            address.setFormattedAddress(result.getString("formatted_address"));
        }
        if (resultAddress.containsKey("street_number")) {
            address.setStreet(resultAddress.getString("street_number"));
        }
        if (resultAddress.containsKey("street_name")) {
            address.setStreet(resultAddress.getString("street_name"));
        }
        if (resultAddress.containsKey("locality")) {
            address.setSettlement(resultAddress.getString("locality"));
        }
        if (resultAddress.containsKey("admin_1")) {
            address.setState(resultAddress.getString("admin_1"));
        }
        if (resultAddress.containsKey("country")) {
            address.setCountry(resultAddress.getString("country"));
        }

        return address;
    }

}
