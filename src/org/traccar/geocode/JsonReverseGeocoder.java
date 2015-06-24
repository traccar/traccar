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

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.traccar.helper.Log;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStreamReader;

public abstract class JsonReverseGeocoder implements ReverseGeocoder {

    private final String url;

    public JsonReverseGeocoder(String url) {
        this.url = url;
    }

    @Override
    public String getAddress(AddressFormat format, double latitude, double longitude) {

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            try {
                CloseableHttpResponse response = httpClient.execute(new HttpGet(String.format(url, latitude, longitude)));
                try {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        JsonReader reader = Json.createReader(new InputStreamReader(entity.getContent()));
                        try {
                            Address address = parseAddress(reader.readObject());
                            if (address != null) {
                                return format.format(address);
                            }
                        } finally {
                            reader.close();
                        }
                    }
                } finally {
                    response.close();
                }
            } finally {
                httpClient.close();
            }
        } catch (Exception error) {
            Log.warning(error);
        }

        return null;
    }

    protected abstract Address parseAddress(JsonObject json);

}
