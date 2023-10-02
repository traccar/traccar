/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import jakarta.ws.rs.client.Client;

public class LocationIqGeocoder extends NominatimGeocoder {

    private static final String DEFAULT_URL = "https://us1.locationiq.com/v1/reverse.php";

    public LocationIqGeocoder(
            Client client, String url, String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(client, url != null ? url : DEFAULT_URL, key, language, cacheSize, addressFormat);
    }

}
