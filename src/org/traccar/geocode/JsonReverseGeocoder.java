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
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

public abstract class JsonReverseGeocoder implements ReverseGeocoder {

    private final String url;

    public JsonReverseGeocoder(String url) {
        this.url = url;
    }

    @Override
    public String getAddress(AddressFormat format, double latitude, double longitude) {

        try {
            URLConnection conn = new URL(String.format(url, latitude, longitude)).openConnection();
            Reader reader = new InputStreamReader(conn.getInputStream());
            try {
                Address address = parseAddress(Json.createReader(reader).readObject());
                if (address != null) {
                    return format.format(address);
                }
            } finally {
                reader.close();
            }
        } catch(Exception error) {
            Log.warning(error);
        }

        return null;
    }

    protected abstract Address parseAddress(JsonObject json);

}
