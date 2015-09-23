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
import javax.json.JsonReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class JsonReverseGeocoder implements ReverseGeocoder {

    private final String url;

    private Map<Map.Entry<Double, Double>, String> cache;

    public JsonReverseGeocoder(String url, final int cacheSize) {
        this.url = url;
        if (cacheSize > 0) {
            this.cache = new LinkedHashMap<Map.Entry<Double, Double>, String>() {
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > cacheSize;
                }
            };
        }
    }

    @Override
    public String getAddress(AddressFormat format, double latitude, double longitude) {

        if (cache != null) {
            String cachedAddress = cache.get(new AbstractMap.SimpleImmutableEntry<>(latitude, longitude));
            if (cachedAddress != null) {
                return cachedAddress;
            }
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(String.format(url, latitude, longitude)).openConnection();
            conn.setRequestProperty("Connection", "close"); // don't keep-alive connections
            try {
                InputStreamReader streamReader = new InputStreamReader(conn.getInputStream());
                try (JsonReader reader = Json.createReader(streamReader)) {
                    Address address = parseAddress(reader.readObject());
                    while (streamReader.read() > 0); // make sure we reached the end
                    if (address != null) {
                        String formattedAddress = format.format(address);

                        if (cache != null) {
                            cache.put(new AbstractMap.SimpleImmutableEntry<>(latitude, longitude), formattedAddress);
                        }

                        return formattedAddress;
                    }
                }
            } finally {
                conn.disconnect();
            }
        } catch(Exception error) {
            Log.warning(error);
        }

        return null;
    }

    protected abstract Address parseAddress(JsonObject json);

}
