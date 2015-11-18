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
package org.traccar.location;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import org.traccar.Context;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class OpenCellIdLocationProvider extends BaseLocationProvider {

    private String url;

    public OpenCellIdLocationProvider(String key) {
        this("http://opencellid.org/cell/get", key);
    }

    public OpenCellIdLocationProvider(String url, String key) {
        this.url = url + "?format=json&mcc=%d&mnc=%d&lac=%d&cellid=%d&key=" + key;
    }

    protected void getLocation(int mcc, int mnc, long lac, long cid, final LocationProviderCallback callback) {
        String x = String.format(url, mcc, mnc, lac, cid);
        Context.getAsyncHttpClient().prepareGet(String.format(url, mcc, mnc, lac, cid))
                .execute(new AsyncCompletionHandler() {
                    @Override
                    public Object onCompleted(Response response) throws Exception {
                        try (JsonReader reader = Json.createReader(response.getResponseBodyAsStream())) {
                            JsonObject json = reader.readObject();
                            if (json.containsKey("lat") && json.containsKey("lon")) {
                                callback.onSuccess(
                                        json.getJsonNumber("lat").doubleValue(),
                                        json.getJsonNumber("lon").doubleValue());
                            } else {
                                callback.onFailure();
                            }
                        }
                        return null;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        callback.onFailure();
                    }
                });
    }

}
