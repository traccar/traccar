/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.traccar.helper.Checksum;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class WebDataHandler extends BaseDataHandler {

    private final String url;

    public WebDataHandler(String url) {
        this.url = url;
    }

    private static String formatSentence(Position position) {

        StringBuilder s = new StringBuilder("$GPRMC,");

        try (Formatter f = new Formatter(s, Locale.ENGLISH)) {

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
            calendar.setTimeInMillis(position.getFixTime().getTime());

            f.format("%1$tH%1$tM%1$tS.%1$tL,A,", calendar);

            double lat = position.getLatitude();
            double lon = position.getLongitude();

            f.format("%02d%07.4f,%c,", (int) Math.abs(lat), Math.abs(lat) % 1 * 60, lat < 0 ? 'S' : 'N');
            f.format("%03d%07.4f,%c,", (int) Math.abs(lon), Math.abs(lon) % 1 * 60, lon < 0 ? 'W' : 'E');

            f.format("%.2f,%.2f,", position.getSpeed(), position.getCourse());
            f.format("%1$td%1$tm%1$ty,,", calendar);
        }

        s.append(Checksum.nmea(s.toString()));

        return s.toString();
    }

    private String calculateStatus(Position position) {
        if (position.getAttributes().containsKey(Position.KEY_ALARM)) {
            return "0xF841"; // STATUS_PANIC_ON
        } else if (position.getSpeed() < 1.0) {
            return "0xF020"; // STATUS_LOCATION
        } else {
            return "0xF11C"; // STATUS_MOTION_MOVING
        }
    }

    public String formatRequest(Position position) {

        Device device = Context.getIdentityManager().getDeviceById(position.getDeviceId());

        String request = url
                .replace("{name}", device.getName())
                .replace("{uniqueId}", device.getUniqueId())
                .replace("{deviceId}", String.valueOf(position.getDeviceId()))
                .replace("{protocol}", String.valueOf(position.getProtocol()))
                .replace("{deviceTime}", String.valueOf(position.getDeviceTime().getTime()))
                .replace("{fixTime}", String.valueOf(position.getFixTime().getTime()))
                .replace("{valid}", String.valueOf(position.getValid()))
                .replace("{latitude}", String.valueOf(position.getLatitude()))
                .replace("{longitude}", String.valueOf(position.getLongitude()))
                .replace("{altitude}", String.valueOf(position.getAltitude()))
                .replace("{speed}", String.valueOf(position.getSpeed()))
                .replace("{course}", String.valueOf(position.getCourse()))
                .replace("{statusCode}", calculateStatus(position));

        if (position.getAddress() != null) {
            try {
                request = request.replace(
                        "{address}", URLEncoder.encode(position.getAddress(), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException error) {
                Log.warning(error);
            }
        }

        if (request.contains("{attributes}")) {
            try {
                String attributes = Context.getObjectMapper().writeValueAsString(position.getAttributes());
                request = request.replace(
                        "{attributes}", URLEncoder.encode(attributes, StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException | JsonProcessingException error) {
                Log.warning(error);
            }
        }

        if (request.contains("{gprmc}")) {
            request = request.replace("{gprmc}", formatSentence(position));
        }

        return request;
    }

    @Override
    protected Position handlePosition(Position position) {

        Context.getAsyncHttpClient().prepareGet(formatRequest(position)).execute();

        return position;
    }

}
