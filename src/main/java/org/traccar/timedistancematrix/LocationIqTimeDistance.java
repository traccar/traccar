/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@traccar.org)
 * Copyright 2019 - Rafael Miquelino (rafaelmiquelino@gmail.com)
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
package org.traccar.timedistancematrix;

import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.ClientErrorException;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class LocationIqTimeDistance extends JsonTimeDistance {
    private final String url;
    private final String key;

    public LocationIqTimeDistance(String url, String key) {
        if (url == null) {
            this.url = "https://us1.locationiq.com/v1/matrix/driving/";
        } else {
            this.url = url;
        }
        this.key = key;
    }

    @Override
    public JsonObject getTimeDistanceResponse(TimeDistanceRequest timeDistanceRequest)
            throws ClientErrorException {
        String metrics = String.join(",", timeDistanceRequest.getMetrics());

        StringJoiner locationsStringJoiner = new StringJoiner(";");
        for (List<Double> coordinates : timeDistanceRequest.getLocations()) {
            StringJoiner locationStringJoiner = new StringJoiner(",");
            for (Double point : coordinates) {
                locationStringJoiner.add(Objects.toString(point));
            }
            locationsStringJoiner.add(locationStringJoiner.toString());
        }
        String locations = locationsStringJoiner.toString();

        StringJoiner sourcesJoiner = new StringJoiner(";");
        for (int source : timeDistanceRequest.getSources()) {
            sourcesJoiner.add(Objects.toString(source));
        }
        String sources = sourcesJoiner.toString();

        String destinations = Objects.toString(timeDistanceRequest.getDestinations().get(0));

        String finalUrl = String.format(
                "%s%s?destinations=%s&sources=%s&annotations=%s&key=%s",
                url,
                locations,
                destinations, sources,
                metrics,
                key);

        return Context
                .getClient()
                .target(finalUrl)
                .request()
                .get(JsonObject.class);
    }
}
