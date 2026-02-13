/*
 * Copyright 2022 - 2024 Anton Tananaev (anton@traccar.org)
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

import com.google.openlocationcode.OpenLocationCode;
import org.traccar.database.StatisticsManager;

public class PlusCodesGeocoder implements Geocoder {

    @Override
    public void setStatisticsManager(StatisticsManager statisticsManager) {
    }

    @Override
    public String getAddress(double latitude, double longitude, ReverseGeocoderCallback callback) {
        String address = new OpenLocationCode(latitude, longitude).getCode();
        if (callback != null) {
            callback.onSuccess(address);
            return null;
        }
        return address;
    }

}
