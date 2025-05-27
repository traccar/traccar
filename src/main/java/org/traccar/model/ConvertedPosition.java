/*
 * Copyright 2025 Haven Madray (sgpublic2002@gmail.com)
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
package org.traccar.model;

import org.traccar.geoconv.PositionConverter;
import org.traccar.storage.StorageName;

@StorageName("tc_converted_positions")
public class ConvertedPosition extends ExtendedModel {
    // these keys should match the map id in the front-end source code
    public static final String PLATFORM_AUTONAVI = "autoNavi";
    public static final String PLATFORM_BAIDU = "baidu";
    public static final String PLATFORM_TENCENT = "tencent";

    public static final String CRS_GCJ_02 = "GCJ_02";
    public static final String CRS_BD_09 = "BD_09";

    public ConvertedPosition() {
    }

    public ConvertedPosition(PositionConverter.ConverterInfo info) {
        this.platform = info.getPlatform();
        this.crs = info.getCrs();
    }

    private String platform;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    private String crs;

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private double latitude;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude out of range");
        }
        this.latitude = latitude;
    }

    private double longitude;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude out of range");
        }
        this.longitude = longitude;
    }
}
