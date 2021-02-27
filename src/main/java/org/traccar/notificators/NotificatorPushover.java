/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;

public class NotificatorPushover extends Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorPushover.class);

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
        @JsonProperty("message")
        private String message;
    }

    public NotificatorPushover() {
        url = "https://api.pushover.net/1/messages.json";
        token = Context.getConfig().getString(Keys.NOTIFICATOR_PUSHOVER_TOKEN);
        user = Context.getConfig().getString(Keys.NOTIFICATOR_PUSHOVER_USER);
    }

    @Override
    public void sendSync(long userId, Event event, Position position) {

        final User user = Context.getPermissionsManager().getUser(userId);

        String device = "";

        if (user.getAttributes().containsKey("notificator.pushover.device")) {
            device = user.getString("notificator.pushover.device").replaceAll(" *, *", ",");
        }

        if (token == null) {
            LOGGER.warn("Pushover token not found");
            return;
        }

        if (this.user == null) {
            LOGGER.warn("Pushover user not found");
            return;
        }

        Message message = new Message();
        message.token = token;
        message.user = this.user;
        message.device = device;
        message.message = NotificationFormatter.formatShortMessage(userId, event, position);

        Context.getClient().target(url).request()
                .async().post(Entity.json(message), new InvocationCallback<Object>() {
            @Override
            public void completed(Object o) {
            }

            @Override
            public void failed(Throwable throwable) {
                LOGGER.warn("Pushover API error", throwable);
            }
        });
    }

    @Override
    public void sendAsync(long userId, Event event, Position position) {
        sendSync(userId, event, position);
    }

}
