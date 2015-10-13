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

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.traccar.Context;

public abstract class JsonReverseGeocoder implements ReverseGeocoder {

    private final String url;

    private Map<Map.Entry<Double, Double>, String> cache;

    public JsonReverseGeocoder(String url, final int cacheSize) {
        this.url = url;
        if (cacheSize > 0) {
            this.cache = Collections.synchronizedMap(new LinkedHashMap<Map.Entry<Double, Double>, String>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > cacheSize;
                }
            });
        }
    }

    @Override
    public void getAddress(
            final AddressFormat format, final double latitude,
            final double longitude, final ReverseGeocoderCallback callback) {

        if (cache != null) {
            String cachedAddress = cache.get(new AbstractMap.SimpleImmutableEntry<>(latitude, longitude));
            if (cachedAddress != null) {
                callback.onResult(cachedAddress);
                return;
            }
        }

        Context.getAsyncHttpClient().prepareGet(String.format(url, latitude, longitude))
                .execute(new AsyncCompletionHandler() {
            @Override
            public Object onCompleted(Response response) throws Exception {
                try (JsonReader reader = Json.createReader(response.getResponseBodyAsStream())) {
                    Address address = parseAddress(reader.readObject());
                    if (address != null) {
                        String formattedAddress = format.format(address);
                        if (cache != null) {
                            cache.put(new AbstractMap.SimpleImmutableEntry<>(latitude, longitude), formattedAddress);
                        }
                        callback.onResult(formattedAddress);
                    } else {
                        callback.onResult(null);
                    }
                }
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                callback.onResult(null);
            }
        });
    }

    public abstract Address parseAddress(JsonObject json);

}
