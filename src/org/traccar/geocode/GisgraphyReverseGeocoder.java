/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.traccar.helper.Log;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class GisgraphyReverseGeocoder implements ReverseGeocoder {

    private final String url;

    public GisgraphyReverseGeocoder() {
        this("http://services.gisgraphy.com/street/streetsearch");
    }

    public GisgraphyReverseGeocoder(String url) {
        this.url = url + "?format=json&lat=%f&lng=%f&from=1&to=1";
    }

    @Override
    public String getAddress(AddressFormat format, double latitude, double longitude) {
        
        try {
            Address address = new Address();
            URLConnection conn = new URL(String.format(url, latitude, longitude)).openConnection();

            JsonObject json = Json.createReader(new InputStreamReader(conn.getInputStream())).readObject();
            JsonObject result = json.getJsonArray("result").getJsonObject(0);

            if (result.containsKey("name")) {
                address.setStreet(result.getString("name"));
            }
            if (result.containsKey("isIn")) {
                address.setSettlement(result.getString("isIn"));
            }
            if (result.containsKey("countryCode")) {
                address.setCountry(result.getString("countryCode"));
            }

            return format.format(address);

        } catch(Exception error) {
            Log.warning(error);
        }

        return null;
    }

}
