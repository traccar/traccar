/*
 * Copyright 2014 - 2015 Stefaan Van Dooren (stefaan.vandooren@gmail.com)
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

public class FactualGeocoder extends JsonGeocoder {

    public FactualGeocoder(String url, String key, int cacheSize, AddressFormat addressFormat) {
        super(url + "?latitude=%f&longitude=%f&KEY=" + key, cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonObject result = json.getJsonObject("response").getJsonObject("data");
        if (result != null) {
                Address address = new Address();
                if (result.getJsonObject("street_number") != null) {
                    address.setHouse(result.getJsonObject("street_number").getString("name"));
                }
                if (result.getJsonObject("street_name") != null) {
                    address.setStreet(result.getJsonObject("street_name").getString("name"));
                }
                if (result.getJsonObject("locality") != null) {
                    address.setSettlement(result.getJsonObject("locality").getString("name"));
                }
                if (result.getJsonObject("county") != null) {
                    address.setDistrict(result.getJsonObject("county").getString("name"));
                }
                if (result.getJsonObject("region") != null) {
                    address.setState(result.getJsonObject("region").getString("name"));
                }
                if (result.getJsonObject("country") != null) {
                    address.setCountry(result.getJsonObject("country").getString("name"));
                }
                if (result.getJsonObject("postcode") != null) {
                    address.setPostcode(result.getJsonObject("postcode").getString("name"));
                }
                return address;
        }
        return null;
    }

}
