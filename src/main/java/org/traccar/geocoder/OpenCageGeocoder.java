/*
 * Copyright 2014 - 2015 Stefaan Van Dooren (stefaan.vandooren@gmail.com)
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
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

public class OpenCageGeocoder extends JsonGeocoder {

    private static String formatUrl(String url, String key, String language) {
        if (url == null) {
            url = "https://api.opencagedata.com/geocode/v1";
        }
        url += "/json?q=%f,%f&no_annotations=1&key=" + key;
        if (language != null) {
            url += "&language=" + language;
        }
        return url;
    }

    public OpenCageGeocoder(String url, String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(formatUrl(url, key, language), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonArray result = json.getJsonArray("results");
        if (result != null) {
            JsonObject location = result.getJsonObject(0).getJsonObject("components");
            if (location != null) {
                Address address = new Address();

                if (result.getJsonObject(0).containsKey("formatted")) {
                    address.setFormattedAddress(result.getJsonObject(0).getString("formatted"));
                }
                if (location.containsKey("building")) {
                    address.setHouse(location.getString("building"));
                }
                if (location.containsKey("house_number")) {
                    address.setHouse(location.getString("house_number"));
                }
                if (location.containsKey("road")) {
                    address.setStreet(location.getString("road"));
                }
                if (location.containsKey("suburb")) {
                    address.setSuburb(location.getString("suburb"));
                }
                if (location.containsKey("city")) {
                    address.setSettlement(location.getString("city"));
                }
                if (location.containsKey("city_district")) {
                    address.setSettlement(location.getString("city_district"));
                }
                if (location.containsKey("county")) {
                    address.setDistrict(location.getString("county"));
                }
                if (location.containsKey("state")) {
                    address.setState(location.getString("state"));
                }
                if (location.containsKey("country_code")) {
                    address.setCountry(location.getString("country_code").toUpperCase());
                }
                if (location.containsKey("postcode")) {
                    address.setPostcode(location.getString("postcode"));
                }

                return address;
            }
        }
        return null;
    }

}
