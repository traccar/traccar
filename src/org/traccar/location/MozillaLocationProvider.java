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
package org.traccar.location;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import org.traccar.Context;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class MozillaLocationProvider extends BaseLocationProvider {

    private static final String URL = "https://location.services.mozilla.com/v1/geolocate";

    private String url;
    private String template;

    public MozillaLocationProvider() {
        this("test");
    }

    public MozillaLocationProvider(String key) {
        this.url = URL + "?key=" + key;

        template = new StringBuilder()
                .append("{\"cellTowers\":[{")
                .append("\"radioType\":\"gsm\",")
                .append("\"mobileCountryCode\":%d,")
                .append("\"mobileNetworkCode\":%d,")
                .append("\"locationAreaCode\":%d,")
                .append("\"cellId\":%d")
                .append("}]}")
                .toString();
    }

    protected void getLocation(int mcc, int mnc, long lac, long cid, final LocationProviderCallback callback) {
        String body = String.format(template, mcc, mnc, lac, cid);
        Context.getAsyncHttpClient().preparePost(url).setBody(body).execute(new AsyncCompletionHandler() {
            @Override
            public Object onCompleted(Response response) throws Exception {
                try (JsonReader reader = Json.createReader(response.getResponseBodyAsStream())) {
                    JsonObject json = reader.readObject().getJsonObject("location");
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
