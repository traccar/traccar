/*
 * Copyright 2020 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.NotificationMessage;

@Singleton
public class NotificatorPushover extends Notificator {

    private final Client client;

    private final String url;
    private final String token;
    private final String user;

    public static class Message {
        @JsonProperty("token")
        private String token;
        @JsonProperty("user")
        private String user;
        @JsonProperty("device")
        private String device;
        @JsonProperty("title")
        private String title;
        @JsonProperty("message")
        private String message;
    }

    @Inject
    public NotificatorPushover(Config config, NotificationFormatter notificationFormatter, Client client) {
        super(notificationFormatter, "short");
        this.client = client;
        url = "https://api.pushover.net/1/messages.json";
        token = config.getString(Keys.NOTIFICATOR_PUSHOVER_TOKEN);
        user = config.getString(Keys.NOTIFICATOR_PUSHOVER_USER);
    }

    @Override
    public void send(User user, NotificationMessage shortMessage, Event event, Position position) {

        Message message = new Message();
        message.token = token;

        message.user = user.getString("pushoverUserKey");
        if (message.user == null) {
            message.user = this.user;
        }

        if (user.hasAttribute("pushoverDeviceNames")) {
            message.device = user.getString("pushoverDeviceNames").replaceAll(" *, *", ",");
        }

        message.title = shortMessage.getSubject();
        message.message = shortMessage.getBody();

        client.target(url).request().post(Entity.json(message)).close();
    }

}
