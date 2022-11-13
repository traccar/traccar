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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;

public class EventForwarderJson implements EventForwarder {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventForwarderJson.class);

    private final String url;
    private final String header;

    private final Client client;

    @Inject
    public EventForwarderJson(Config config, Client client) {
        this.client = client;
        url = config.getString(Keys.EVENT_FORWARD_URL);
        header = config.getString(Keys.EVENT_FORWARD_HEADERS);
    }

    public void forward(EventData eventData) {
        var requestBuilder = client.target(url).request();

        if (header != null && !header.isEmpty()) {
            for (String line: header.split("\\r?\\n")) {
                String[] values = line.split(":", 2);
                requestBuilder.header(values[0].trim(), values[1].trim());
            }
        }

        requestBuilder.async().post(Entity.json(eventData), new InvocationCallback<>() {
            @Override
            public void completed(Object o) {
            }

            @Override
            public void failed(Throwable throwable) {
                LOGGER.warn("Event forwarding failed", throwable);
            }
        });
    }

}
