/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.geocode;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import org.traccar.helper.Log;

public class GoogleReverseGeocoder extends JsonReverseGeocoder {

    public GoogleReverseGeocoder() {
        super("http://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f");
    }

    @Override
    protected Address parseAddress(JsonObject json) {
        JsonArray results = json.getJsonArray("results");

        if (!results.isEmpty()) {
            Address address = new Address();

            JsonObject result = (JsonObject) results.get(0);
            JsonArray components = result.getJsonArray("address_components");

            for (JsonObject component : components.getValuesAs(JsonObject.class)) {

                String value = component.getString("short_name");

                for (JsonString type : component.getJsonArray("types").getValuesAs(JsonString.class)) {
                    if (type.getString().equals("street_number")) {
                        address.setHouse(value);
                        break;
                    } else if (type.getString().equals("route")) {
                        address.setStreet(value);
                        break;
                    } else if (type.getString().equals("locality")) {
                        address.setSettlement(value);
                        break;
                    } else if (type.getString().equals("administrative_area_level_2")) {
                        address.setDistrict(value);
                        break;
                    } else if (type.getString().equals("administrative_area_level_1")) {
                        address.setState(value);
                        break;
                    } else if (type.getString().equals("country")) {
                        address.setCountry(value);
                        break;
                    } else if (type.getString().equals("postal_code")) {
                        address.setPostcode(value);
                        break;
                    }
                }
            }

            return address;
        }

        return null;
    }

}
