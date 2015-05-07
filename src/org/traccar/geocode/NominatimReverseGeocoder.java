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

public class NominatimReverseGeocoder implements ReverseGeocoder {

    private final String url;

    public NominatimReverseGeocoder() {
        this("http://nominatim.openstreetmap.org/reverse");
    }
    
    public NominatimReverseGeocoder(String url) {
        this.url = url + "?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1";
    }

    @Override
    public String getAddress(AddressFormat format, double latitude, double longitude) {
        
        try {
            Address address = new Address();
            URLConnection conn = new URL(String.format(url, latitude, longitude)).openConnection();

            JsonObject json = Json.createReader(new InputStreamReader(conn.getInputStream())).readObject().getJsonObject("address");

            if (json.containsKey("house_number")) {
                address.setHouse(json.getString("house_number"));
            }
            if (json.containsKey("road")) {
                address.setStreet(json.getString("road"));
            }
            if (json.containsKey("village")) {
                address.setSettlement(json.getString("village"));
            }
            if (json.containsKey("city")) {
                address.setSettlement(json.getString("city"));
            }
            if (json.containsKey("state_district")) {
                address.setDistrict(json.getString("state_district"));
            }
            if (json.containsKey("state")) {
                address.setState(json.getString("state"));
            }
            if (json.containsKey("country_code")) {
                address.setCountry(json.getString("country_code").toUpperCase());
            }
            if (json.containsKey("postcode")) {
                address.setPostcode(json.getString("postcode"));
            }

            return format.format(address);

        } catch(Exception error) {
            Log.warning(error);
        }

        return null;
    }

}
