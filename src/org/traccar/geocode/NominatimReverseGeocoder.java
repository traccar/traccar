/*
 * Copyright 2014 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.traccar.helper.Log;
import org.w3c.dom.Document;

public class NominatimReverseGeocoder extends JsonReverseGeocoder {

    public NominatimReverseGeocoder() {
        this("http://nominatim.openstreetmap.org/reverse");
    }
    
    public NominatimReverseGeocoder(String url) {
        super(url + "?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1");
    }

    @Override
    protected Address parseAddress(JsonObject json) {
        JsonObject result = json.getJsonObject("address");

        if (result != null) {
            Address address = new Address();

            if (result.containsKey("house_number")) {
                address.setHouse(result.getString("house_number"));
            }
            if (result.containsKey("road")) {
                address.setStreet(result.getString("road"));
            }
            if (result.containsKey("village")) {
                address.setSettlement(result.getString("village"));
            }
            if (result.containsKey("city")) {
                address.setSettlement(result.getString("city"));
            }
            if (result.containsKey("state_district")) {
                address.setDistrict(result.getString("state_district"));
            }
            if (result.containsKey("state")) {
                address.setState(result.getString("state"));
            }
            if (result.containsKey("country_code")) {
                address.setCountry(result.getString("country_code").toUpperCase());
            }
            if (result.containsKey("postcode")) {
                address.setPostcode(result.getString("postcode"));
            }

            return address;
        }

        return null;
    }

}
