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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Entity;

public class OpenRouteServiceTimeDistance extends JsonTimeDistance {
    public OpenRouteServiceTimeDistance(String url, String key) {
        super(url, key);
    }

    @Override
    public JsonObject getTimeDistanceResponse(String url, String key, TimeDistanceRequest timeDistanceRequest)
            throws ClientErrorException {
        if (url == null) {
            url = "https://api.openrouteservice.org/v2/matrix/driving-car";
        }

        String requestBodyString = null;
        try {
            requestBodyString = Context.getObjectMapper().writeValueAsString(timeDistanceRequest);
        } catch (JsonProcessingException e) {
            LOGGER.warn(String.valueOf(e));
        }

        Entity<String> requestBodyEntity = Entity.json(requestBodyString);

        return Context
                .getClient()
                .target(url)
                .request()
                .header("Authorization", key)
                .header("Accept",
                        "application/json; charset=utf-8")
                .post(requestBodyEntity)
                .readEntity(JsonObject.class);

    }
}
