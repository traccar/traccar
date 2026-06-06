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

public class BaiduGeocoder extends JsonGeocoder {

    private static String formatUrl(String key, String language) {
        String url = "https://api.map.baidu.com/reverse_geocoding/v3/"
                + "?output=json&location=%f,%f&coordtype=gcj02ll&extensions_poi=1";
        if (key != null) {
            url += "&ak=" + key;
        }
        if (language != null) {
            url += "&language=" + language + "&language_auto=1";
        }
        return url;
    }

    public BaiduGeocoder(Client client, String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key, language), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonObject result = readObject(json, "result");
        if (result == null) {
            return null;
        }
        Address address = new Address();
        address.setFormattedAddress(readValue(result, "formatted_address"));

        JsonArray pois = readArray(result, "pois");
        if (pois != null && !pois.isEmpty()) {
            address.setHouse(readValue(pois.getJsonObject(0), "name"));
        }

        JsonObject addressComponent = readObject(result, "addressComponent");
        if (addressComponent != null) {
            address.setStreet(readValue(addressComponent, "street"));
            address.setSuburb(readValue(addressComponent, "town"));
            address.setDistrict(readValue(addressComponent, "district"));
            address.setSettlement(readValue(addressComponent, "city"));
            address.setState(readValue(addressComponent, "province"));
            address.setCountry(readValue(addressComponent, "country"));
        }
        return address;
    }

    @Override
    protected String parseError(JsonObject json) {
        return readValue(json, "message");
    }

    @Override
    public String getAddress(
            final double latitude, final double longitude, final ReverseGeocoderCallback callback) {
        CoordinateUtil.Coordinate coordinate = CoordinateUtil.wgs84ToGcj02(latitude, longitude);
        return super.getAddress(coordinate.latitude(), coordinate.longitude(), callback);
    }

}
