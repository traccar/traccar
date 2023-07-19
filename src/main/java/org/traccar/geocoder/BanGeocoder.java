/*
 * Copyright 2018 Olivier Girondel (olivier@biniou.info)
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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

/*
 * API documentation: https://adresse.data.gouv.fr/api
 */

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;

public class BanGeocoder extends JsonGeocoder {

    public BanGeocoder(Client client, int cacheSize, AddressFormat addressFormat) {
        super(client, "https://api-adresse.data.gouv.fr/reverse/?lat=%f&lon=%f", cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonArray result = json.getJsonArray("features");

        if (result != null && !result.isEmpty()) {
            JsonObject location = result.getJsonObject(0).getJsonObject("properties");
            Address address = new Address();

            address.setCountry("FR");
            if (location.containsKey("postcode")) {
                address.setPostcode(location.getString("postcode"));
            }
            if (location.containsKey("context")) {
                address.setDistrict(location.getString("context"));
            }
            if (location.containsKey("name")) {
                address.setStreet(location.getString("name"));
            }
            if (location.containsKey("city")) {
                address.setSettlement(location.getString("city"));
            }
            if (location.containsKey("housenumber")) {
                address.setHouse(location.getString("housenumber"));
            }
            if (location.containsKey("label")) {
                address.setFormattedAddress(location.getString("label"));
            }

            return address;
        }

        return null;
    }

}
