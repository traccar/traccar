/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;


public class AmapGeocoder extends JsonGeocoder {

    private static final double pi = 3.1415926535897932384626;
    private static final double a = 6378245.0;
    private static final double ee = 0.00669342162296594323;

    private static String formatUrl(String key) {
        String url = "https://restapi.amap.com/v3/geocode/regeo?parameters?output=json&location=%2$f,%1$f&extensions=all";
        if (key != null) {
            url += "&key=" + key;
        }
        return url;
    }

    public AmapGeocoder(Client client, String key, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key), cacheSize, addressFormat);
    }

    public Address parseAddress(JsonObject json) {
        JsonObject regeocode = json.getJsonObject("regeocode");
        if (regeocode != null) {
            Address address = new Address();

            address.setFormattedAddress(getJsonNode(regeocode, "formatted_address"));

            JsonObject addressComponent = regeocode.getJsonObject("addressComponent");
            if (addressComponent != null) {
                JsonObject building = addressComponent.getJsonObject("building");
                String houseName = getJsonNode(building, "name");
                if (houseName == null) {
                    JsonArray aois = regeocode.getJsonArray("aois");
                    if (!aois.isEmpty()) {
                        JsonObject aoi = aois.getJsonObject(0);
                        houseName = getJsonNode(aoi, "name");
                    }
                }
                address.setHouse(houseName);

                JsonObject street = addressComponent.getJsonObject("streetNumber");
                address.setStreet(getJsonNode(street, "street"));

                address.setSuburb(getJsonNode(addressComponent, "township"));
                address.setSettlement(getJsonNode(addressComponent, "district"));
                address.setDistrict(getJsonNode(addressComponent, "city"));
                address.setState(getJsonNode(addressComponent, "province"));
                address.setCountry(getJsonNode(addressComponent, "country"));
            }
            return address;
        }
        return null;
    }

    private String getJsonNode(JsonObject json, String key) {
        if (json != null && json.containsKey(key)) {
            JsonValue value = json.get(key);
            if (value.getValueType() == JsonValue.ValueType.STRING) {
                return ((JsonString)value).getString();
            }
        }
        return null;
    }

    protected String parseError(JsonObject json) {
        return json.getString("info");
    }

    @Override
    public String getAddress(
            final double latitude, final double longitude, final ReverseGeocoderCallback callback) {
        if (outOfChina(latitude, longitude)) {
            return null;
        }
        double dLat = transformLat(longitude - 105.0, latitude - 35.0);
        double dLon = transformLon(longitude - 105.0, latitude - 35.0);
        double radLat = latitude / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = latitude + dLat;
        double mgLon = longitude + dLon;

        return super.getAddress(mgLat, mgLon, callback);
    }

    private static boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
                * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0
                * pi)) * 2.0 / 3.0;
        return ret;
    }
}
