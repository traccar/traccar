/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.reports.common;

import jakarta.annotation.Nullable;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.geocoder.Geocoder;

/**
 * Resolves address for report positions, using existing address or geocoder when configured.
 */
public class ReportAddressResolver {

    private final Config config;
    private final Geocoder geocoder;

    public ReportAddressResolver(Config config, @Nullable Geocoder geocoder) {
        this.config = config;
        this.geocoder = geocoder;
    }

    /**
     * Returns the address for the given coordinates, using existing address if present,
     * otherwise geocoding when geocoder is available and geocoder-on-request is enabled.
     */
    @Nullable
    public String resolveAddress(double latitude, double longitude, @Nullable String existingAddress) {
        if (existingAddress != null) {
            return existingAddress;
        }
        boolean geocoderEnabled = geocoder != null && config.getBoolean(Keys.GEOCODER_ON_REQUEST);
        if (geocoderEnabled) {
            return geocoder.getAddress(latitude, longitude, null);
        }
        return null;
    }
}
