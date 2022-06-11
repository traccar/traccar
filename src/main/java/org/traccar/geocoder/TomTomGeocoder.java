/*
 * Copyright 2020 Przemek Malolepszy (szogoon@gmail.com)
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

public class TomTomGeocoder extends JsonGeocoder {

    private static String formatUrl(String url, String key) {
        if (url == null) {
            url = "https://api.tomtom.com/search/2/reverseGeocode/";
        }
        url += "%f,%f.json?key=" + key;
        return url;
    }

    public TomTomGeocoder(Client client, String url, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(url, key), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonArray addresses = json.getJsonArray("addresses");
        if (addresses != null) {
            JsonObject record = addresses.getJsonObject(0);
            if (record != null) {
                JsonObject location = record.getJsonObject("address");

                Address address = new Address();

                if (location.containsKey("streetNumber")) {
                    address.setHouse(location.getString("streetNumber"));
                }
                if (location.containsKey("street")) {
                    address.setStreet(location.getString("street"));
                }
                if (location.containsKey("municipality")) {
                    address.setSettlement(location.getString("municipality"));
                }
                if (location.containsKey("municipalitySubdivision")) {
                    address.setDistrict(location.getString("municipalitySubdivision"));
                }
                if (location.containsKey("countrySubdivision")) {
                    address.setState(location.getString("countrySubdivision"));
                }
                if (location.containsKey("country")) {
                    address.setCountry(location.getString("country").toUpperCase());
                }
                if (location.containsKey("postalCode")) {
                    address.setPostcode(location.getString("postalCode"));
                }

                return address;
            }
        }
        return null;
    }

}
