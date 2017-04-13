/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import org.traccar.Context;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class JsonGeocoder implements Geocoder {

    private final String url;

    private Map<Map.Entry<Double, Double>, String> cache;

    public JsonGeocoder(String url, final int cacheSize) {
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
                callback.onSuccess(cachedAddress);
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
                        callback.onSuccess(formattedAddress);
                    } else {
                        callback.onFailure(new GeocoderException("Empty address"));
                    }
                }
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public abstract Address parseAddress(JsonObject json);

}
