/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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

public class PositionStackGeocoder extends JsonGeocoder {

    private static String formatUrl(String key) {
        return "http://api.positionstack.com/v1/reverse?access_key=" + key + "&query=%f,%f";
    }

    public PositionStackGeocoder(Client client, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key), cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonArray result = json.getJsonArray("data");

        if (result != null && !result.isEmpty()) {
            JsonObject record = result.getJsonObject(0);

            Address address = new Address();

            address.setFormattedAddress(readValue(record, "label"));
            address.setHouse(readValue(record, "number"));
            address.setStreet(readValue(record, "street"));
            address.setSuburb(readValue(record, "neighbourhood"));
            address.setSettlement(readValue(record, "locality"));
            address.setState(readValue(record, "region"));
            address.setCountry(readValue(record, "country_code"));
            address.setPostcode(readValue(record, "postal_code"));

            return address;
        }

        return null;
    }

}
