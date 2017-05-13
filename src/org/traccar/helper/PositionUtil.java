/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *                Ivan F. Martinez
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
package org.traccar.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.MiscFormatter;
import org.traccar.model.Position;

public final class PositionUtil {

    public static final String GMAPS_URL = "https://www.google.com/maps/place/{latitude},{longitude}";

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public static String processTemplate(final String template, final Position position, Map<String,String> extraValues, boolean isURL) {
        //TODO merge with WebDataHandler code, only statusCode and gprmc is not here, and should be passed as extraValues

        final Device device = Context.getIdentityManager().getDeviceById(position.getDeviceId());
        final DateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX");
        final DateFormat ISO8601Z = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        ISO8601Z.setTimeZone(UTC);
        @SuppressWarnings("serial")
        final Map<String,String> replaceMap = new HashMap<String,String>() {
            {{
                    put("{uniqueId}", device.getUniqueId());
                    put("{deviceId}", String.valueOf(position.getDeviceId()));
                    put("{deviceName}", device.getName());
                    put("{protocol}", String.valueOf(position.getProtocol()));

                    if (position.getDeviceTime() != null) {
                        put("{deviceTime}", String.valueOf(position.getDeviceTime().getTime()));
                        put("{deviceTimeISO}", ISO8601.format(position.getDeviceTime()));
                        put("{deviceTimeISOZ}", ISO8601Z.format(position.getDeviceTime()));
                    } else {
                        put("{deviceTime}", "");
                        put("{deviceTimeISO}", "");
                        put("{deviceTimeISOZ}", "");

                    }

                    if (position.getFixTime() != null) {
                        put("{fixTime}", String.valueOf(position.getFixTime().getTime()));
                        put("{fixTimeISO}", ISO8601.format(position.getFixTime()));
                        put("{fixTimeISOZ}", ISO8601Z.format(position.getFixTime()));
                    } else {
                        put("{fixTime}", "");
                        put("{fixTimeISO}", "");
                        put("{fixTimeISOZ}", "");
                    }
                    
                    put("{valid}", String.valueOf(position.getValid()));
                    put("{latitude}", String.valueOf(position.getLatitude()));;
                    put("{longitude}", String.valueOf(position.getLongitude()));
                    put("{altitude}", String.valueOf(position.getAltitude()));
                    put("{speed}", String.valueOf(position.getSpeed()));
                    put("{course}", String.valueOf(position.getCourse()));
                    put("{address}", (position.getAddress() != null) ? position.getAddress() : "");

                    put("{alarm}", ""+position.getAttributes().get(Event.KEY_ALARM));
                    if (template.contains("{attributes}")) {
                        // expensive to do every time....
                        final String attributes = MiscFormatter.toJsonString(position.getAttributes());
                        put("{attributes}", attributes);
                    }

                }
            }
        };
        if (extraValues != null) {
            replaceMap.putAll(extraValues);
        }
        String result = template;
        for (String key : replaceMap.keySet()) {
            if (isURL) {
                try {
                    result = result.replace(key, URLEncoder.encode(replaceMap.get(key), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    Log.warning(e);
                }
            } else {
                final String value = replaceMap.get(key);
                result = result.replace(key, (value != null) ? value : "");
            }
        }
        return result;
    }

}
