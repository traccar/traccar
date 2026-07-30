/*
 * Copyright 2024 容均致 (harryrong@rushanio.com)
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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import org.traccar.helper.CoordinateUtil;

public class AutoNaviGeocoder extends JsonGeocoder {

    private static String formatUrl(String key) {
        String url = "https://restapi.amap.com/v3/geocode/regeo?output=json&location=%2$f,%1$f&extensions=all";
        if (key != null) {
            url += "&key=" + key;
        }
        return url;
    }

    public AutoNaviGeocoder(Client client, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonObject regeocode = readObject(json, "regeocode");
        if (regeocode == null) {
            return null;
        }
        Address address = new Address();
        address.setFormattedAddress(readValue(regeocode, "formatted_address"));

        JsonObject addressComponent = readObject(regeocode, "addressComponent");
        if (addressComponent != null) {
            String houseName = readValue(readObject(addressComponent, "building"), "name");
            if (houseName == null) {
                JsonArray aois = readArray(regeocode, "aois");
                if (aois != null && !aois.isEmpty()) {
                    houseName = readValue(aois.getJsonObject(0), "name");
                }
            }
            if (houseName == null) {
                JsonArray pois = readArray(regeocode, "pois");
                if (pois != null && !pois.isEmpty()) {
                    houseName = readValue(pois.getJsonObject(0), "name");
                }
            }
            address.setHouse(houseName);

            address.setStreet(readValue(readObject(addressComponent, "streetNumber"), "street"));
            address.setSuburb(readValue(addressComponent, "township"));
            address.setDistrict(readValue(addressComponent, "district"));
            address.setSettlement(readValue(addressComponent, "city"));
            address.setState(readValue(addressComponent, "province"));
            address.setCountry(readValue(addressComponent, "country"));
        }
        return address;
    }

    @Override
    protected String parseError(JsonObject json) {
        return readValue(json, "info");
    }

    @Override
    public String getAddress(
            final double latitude, final double longitude, final ReverseGeocoderCallback callback) {
        CoordinateUtil.Coordinate coordinate = CoordinateUtil.wgs84ToGcj02(latitude, longitude);
        return super.getAddress(coordinate.latitude(), coordinate.longitude(), callback);
    }

}
