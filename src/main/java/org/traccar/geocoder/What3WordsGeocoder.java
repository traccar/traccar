/*
 * Copyright 2020 Daniel Lintott (daniel.j.lintott@gmail.com)
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

public class What3WordsGeocoder extends JsonGeocoder {

    private static String formatUrl(String key, String language) {
        String url = "https://api.what3words.com/v3/convert-to-3wa";
        url += "?coordinates=%f,%f&key=" + key;

        if (language != null) {
            url += "&language=" + language;
        }

        return url;
    }
    public What3WordsGeocoder(String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(formatUrl(key, language), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        Address address = new Address();
        if(json.containsKey("words")) {
            String w3wAddress = "<span style='color: #E11F26'>///</span><a href='" + json.getString("map") + "' target='_blank'>";
            w3wAddress += json.getString("words");
            w3wAddress += "</a>";
            address.setFormattedAddress(w3wAddress);
        }

        return address;
    }
}
