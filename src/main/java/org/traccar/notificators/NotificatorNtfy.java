/*
 * Copyright 2017 - 2024 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.notificators;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Main;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.NotificationMessage;

import java.util.Objects;

@Singleton
public class NotificatorNtfy extends Notificator {
    private final Client client;
    private final String url;
    private final String token;
    private final String topic;

    //        see: https://docs.ntfy.sh/subscribe/api/#json-message-format
    public static class Message {
        @JsonProperty("time")
        public long time;
        @JsonProperty("topic")
        public String topic;
        @JsonProperty("title")
        public String title;
        @JsonProperty("message")
        public String message;
        @JsonProperty("priority")
        public int priority;

        public Message(long time, String topic, String title, String message, int priority) {
            this.time = time;
            this.topic = topic;
            this.title = title;
            this.message = message;
            this.priority = priority;
        }
    }

    @Inject
    public NotificatorNtfy(
            Config config,
            NotificationFormatter notificationFormatter,
            Client client
    ) {
        super(notificationFormatter);
        this.client = client;
        url = config.getString(Keys.NOTIFICATOR_NTFY_HTTP_URL);
        token = config.getString(Keys.NOTIFICATOR_NTFY_HTTP_TOKEN);
        topic = config.getString(Keys.NOTIFICATOR_NTFY_TOPIC);
    }

    @Override
    public void send(User user, NotificationMessage message, Event event, Position position) throws MessageException {
        Invocation.Builder request = client.target(url).request();
        if (this.token != null) {
            // see: https://docs.ntfy.sh/publish/#authentication
            request = request.header("Authorization", "Bearer " + token);
        }

        String topicToUse = Objects.requireNonNullElseGet(
                user.getString("ntfyTopic"),
                () -> topic
                        .replace("{uid}", String.valueOf(user.getId()))
                        .replace("{email_base32}", new Base32().encodeAsString(
                                user.getEmail().getBytes()).replace("=", "")));

        try (Response response = request.post(Entity.json(new Message(
                System.currentTimeMillis(),
                topicToUse,
                message.subject(),
                message.digest(),
                message.priority() ? 5 : 3
        )))) {
            if (response.getStatus() / 100 != 2) {
                throw new MessageException(response.readEntity(String.class));
            }
        }
    }
}
