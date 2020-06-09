/*
 * Copyright 2020 Mac Zhou (mac_zhou@live.com)
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

public class AutoNaviGeocoder extends JsonGeocoder {

    private static String formatUrl(String url, String key) {
        if (url == null) {
            url = "https://restapi.amap.com/v3/geocode/regeo";
        }
        url += "?output=json&location=%f,%f&key=" + key;
        return url;
    }

    public AutoNaviGeocoder(String url, String key, int cacheSize, AddressFormat addressFormat) {
        super(formatUrl(url, key), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonObject regeocode = json.getJsonObject("regeocode");
        if (regeocode != null) {
            JsonObject addressComponent = regeocode.getJsonObject("addressComponent");
            if (addressComponent != null) {
                Address address = new Address();

                if (regeocode.containsKey("formatted_address")) {
                    address.setFormattedAddress(regeocode.getString("formatted_address"));
                }
                if (addressComponent.containsKey("district")) {
                    address.setDistrict(addressComponent.getString("district"));
                }
                if (addressComponent.containsKey("city")) {
                    address.setSettlement(addressComponent.getString("city"));
                }
                if (addressComponent.containsKey("province")) {
                    address.setState(addressComponent.getString("province"));
                }
                if (addressComponent.containsKey("country")) {
                    address.setCountry(addressComponent.getString("country"));
                }

                return address;
            }
        }
        return null;
    }

}
