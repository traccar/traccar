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

public class TencentGeocoder extends JsonGeocoder {

    private static String formatUrl(String key) {
        String url = "https://apis.map.qq.com/ws/geocoder/v1/?location=%f,%f&get_poi=1&output=json";
        if (key != null) {
            url += "&key=" + key;
        }
        return url;
    }

    public TencentGeocoder(Client client, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonObject result = readObject(json, "result");
        if (result == null) {
            return null;
        }
        Address address = new Address();
        address.setFormattedAddress(readValue(result, "address"));

        JsonArray pois = readArray(result, "pois");
        if (pois != null && !pois.isEmpty()) {
            address.setHouse(readValue(pois.getJsonObject(0), "title"));
        }

        JsonObject addressComponent = readObject(result, "address_component");
        if (addressComponent != null) {
            address.setStreet(readValue(addressComponent, "street"));
            address.setDistrict(readValue(addressComponent, "district"));
            address.setSettlement(readValue(addressComponent, "city"));
            address.setState(readValue(addressComponent, "province"));
            address.setCountry(readValue(addressComponent, "nation"));
        }

        JsonObject addressReference = readObject(result, "address_reference");
        address.setSuburb(readValue(readObject(addressReference, "town"), "title"));

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
