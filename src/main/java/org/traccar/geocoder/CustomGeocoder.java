/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public  class CustomGeocoder implements Geocoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomGeocoder.class);

    private final String url;

    private Map<Map.Entry<Double, Double>, String> cache;

    public CustomGeocoder(String url, int cacheSize) {
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

    protected String readValue(JsonObject object, String key) {
        if (object.containsKey(key) && !object.isNull(key)) {
            return object.getString(key);
        }
        return null;
    }

    private String handleResponse(
            double latitude, double longitude, String json, ReverseGeocoderCallback callback) {

        if (json != null) {
            String formattedAddress = json.replace(";;", ";").replace(";", ", ");
            formattedAddress = formattedAddress.substring(0, formattedAddress.length()-1);
            if (cache != null) {
                cache.put(new AbstractMap.SimpleImmutableEntry<>(latitude, longitude), formattedAddress);
            }
            if (callback != null) {
                callback.onSuccess(formattedAddress);
            }
            return formattedAddress;
        } else {
            String msg = "Empty address. Error: " + json;
            if (callback != null) {
                callback.onFailure(new GeocoderException(msg));
            } else {
                LOGGER.warn(msg);
            }
        }
        return null;
    }

    @Override
    public String getAddress(
            final double latitude, final double longitude, final ReverseGeocoderCallback callback) {

        if (cache != null) {
            String cachedAddress = cache.get(new AbstractMap.SimpleImmutableEntry<>(latitude, longitude));
            if (cachedAddress != null) {
                if (callback != null) {
                    callback.onSuccess(cachedAddress);
                }
                return cachedAddress;
            }
        }

        Invocation.Builder request = Context.getClient().target(String.format(url, latitude, longitude)).request();

        if (callback != null) {
            request.async().get(new InvocationCallback<String>() {
                @Override
                public void completed(String json) {
                    handleResponse(latitude, longitude, json, callback);
                }

                @Override
                public void failed(Throwable throwable) {
                    callback.onFailure(throwable);
                }
            });
        } else {
            try {
                return handleResponse(latitude, longitude, request.get(String.class), null);
            } catch (ClientErrorException e) {
                LOGGER.warn("Geocoder network error", e);
            }
        }
        return null;
    }

    private Address parseAddress(JsonObject json) {
        return null;
    }

    protected String parseError(JsonObject json) {
        return null;
    }

}
