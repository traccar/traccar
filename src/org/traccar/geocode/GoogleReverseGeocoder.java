/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import org.traccar.helper.Log;

/**
 * Reverse geocoder implementation using Google
 */
public class GoogleReverseGeocoder implements ReverseGeocoder {

    private final static String MARKER = "\"formatted_address\" : \"";

    /**
     * Get address string by coordinates
     */
    public String getAddress(double latitude, double longitude) {

        try {
            URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude + "&sensor=false");
            URLConnection connection = url.openConnection();

            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),Charset.forName("UTF-8")));

            // Find address line
            String line;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf(MARKER);
                if (index != -1) {
                    return line.substring(index + MARKER.length(), line.length() - 2);
                }
            }

            reader.close();

        } catch(Exception error) {
            Log.warning(error.getMessage());
        }

        return null;
    }

}
