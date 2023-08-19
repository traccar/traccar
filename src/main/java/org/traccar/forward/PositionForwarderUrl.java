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
package org.traccar.forward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.Checksum;
import org.traccar.model.Device;
import org.traccar.model.Position;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class PositionForwarderUrl implements PositionForwarder {

    private final String url;
    private final String header;

    private final Client client;
    private final ObjectMapper objectMapper;

    public PositionForwarderUrl(Config config, Client client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.url = config.getString(Keys.FORWARD_URL);
        this.header = config.getString(Keys.FORWARD_HEADER);
    }

    @Override
    public void forward(PositionData positionData, ResultHandler resultHandler) {
        try {
            String url = formatRequest(positionData);
            var requestBuilder = client.target(url).request();

            if (header != null && !header.isEmpty()) {
                for (String line: header.split("\\r?\\n")) {
                    String[] values = line.split(":", 2);
                    String headerName = values[0].trim();
                    String headerValue = values[1].trim();
                    requestBuilder.header(headerName, headerValue);
                }
            }

            requestBuilder.async().get(new InvocationCallback<Response>() {
                @Override
                public void completed(Response response) {
                    if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                        resultHandler.onResult(true, null);
                    } else {
                        int code = response.getStatusInfo().getStatusCode();
                        resultHandler.onResult(false, new RuntimeException("HTTP code " + code));
                    }
                }

                @Override
                public void failed(Throwable throwable) {
                    resultHandler.onResult(false, throwable);
                }
            });
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            resultHandler.onResult(false, e);
        }
    }

    public String formatRequest(
            PositionData positionData) throws UnsupportedEncodingException, JsonProcessingException {

        Position position = positionData.getPosition();
        Device device = positionData.getDevice();

        String request = url
                .replace("{name}", URLEncoder.encode(device.getName(), StandardCharsets.UTF_8))
                .replace("{uniqueId}", device.getUniqueId())
                .replace("{status}", device.getStatus())
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
                .replace("{accuracy}", String.valueOf(position.getAccuracy()))
                .replace("{statusCode}", calculateStatus(position));

        if (position.getAddress() != null) {
            request = request.replace(
                    "{address}", URLEncoder.encode(position.getAddress(), StandardCharsets.UTF_8));
        }

        if (request.contains("{attributes}")) {
            String attributes = objectMapper.writeValueAsString(position.getAttributes());
            request = request.replace(
                    "{attributes}", URLEncoder.encode(attributes, StandardCharsets.UTF_8));
        }

        if (request.contains("{gprmc}")) {
            request = request.replace("{gprmc}", formatSentence(position));
        }

        return request;
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

        s.append(Checksum.nmea(s.substring(1)));

        return s.toString();
    }

    // OpenGTS status code
    private String calculateStatus(Position position) {
        if (position.hasAttribute(Position.KEY_ALARM)) {
            return "0xF841"; // STATUS_PANIC_ON
        } else if (position.getSpeed() < 1.0) {
            return "0xF020"; // STATUS_LOCATION
        } else {
            return "0xF11C"; // STATUS_MOTION_MOVING
        }
    }

}
