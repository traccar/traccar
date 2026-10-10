/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import java.util.Locale;

public class MyGeocodeGeocoder extends JsonGeocoder {

    private static String formatUrl(String url, String key, String language) {
        if (url == null) {
            url = "https://api.mygeocode.com/v1/reverse";
        }
        url += "?lat=%f&lon=%f";
        if (key != null && !key.isEmpty()) {
            url += "&key=" + key;
        }
        if (language != null && !language.isEmpty()) {
            url += "&lang=" + language;
        }
        return url;
    }

    public MyGeocodeGeocoder(
            Client client, String url, String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(url, key, language), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonObject result = readObject(json, "result");
        if (result == null) {
            return null;
        }
        Address address = new Address();
        address.setFormattedAddress(readValue(result, "formatted"));

        JsonObject components = readObject(result, "components");
        if (components != null) {
            address.setHouse(readValue(components, "house_number"));
            address.setStreet(readValue(components, "road"));
            address.setSuburb(readValue(components, "suburb"));
            address.setSettlement(readValue(components, "city"));
            address.setDistrict(readValue(components, "county"));
            address.setState(readValue(components, "state"));
            address.setPostcode(readValue(components, "postcode"));
            String countryCode = readValue(components, "country_code");
            if (countryCode != null) {
                address.setCountry(countryCode.toUpperCase(Locale.ROOT));
            }
        }
        return address;
    }

}
