/*
 * Copyright 2018 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;

public class NotificatorFirebase implements Notificator {

    private final NotificationFormatter notificationFormatter;
    private final Client client;

    private final String url;
    private final String key;

    public static class Notification {
        @JsonProperty("title")
        private String title;
        @JsonProperty("body")
        private String body;
        @JsonProperty("sound")
        private String sound;
    }

    public static class Message {
        @JsonProperty("registration_ids")
        private String[] tokens;
        @JsonProperty("notification")
        private Notification notification;
    }

    @Inject
    public NotificatorFirebase(Config config, NotificationFormatter notificationFormatter, Client client) {
        this(
                notificationFormatter, client, "https://fcm.googleapis.com/fcm/send",
                config.getString(Keys.NOTIFICATOR_FIREBASE_KEY));
    }

    protected NotificatorFirebase(
            NotificationFormatter notificationFormatter, Client client, String url, String key) {
        this.notificationFormatter = notificationFormatter;
        this.client = client;
        this.url = url;
        this.key = key;
    }

    @Override
    public void send(User user, Event event, Position position) {
        if (user.getAttributes().containsKey("notificationTokens")) {

            var shortMessage = notificationFormatter.formatMessage(user, event, position, "short");

            Notification notification = new Notification();
            notification.title = shortMessage.getSubject();
            notification.body = shortMessage.getBody();
            notification.sound = "default";

            Message message = new Message();
            message.tokens = user.getString("notificationTokens").split("[, ]");
            message.notification = notification;

            client.target(url).request().header("Authorization", "key=" + key).post(Entity.json(message)).close();
        }
    }

}
