/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.notification.NotificationFormatter;

import org.traccar.model.User;
import org.traccar.notification.PropertiesProvider;
import java.util.Properties;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;

public class NotificatorPushover extends Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorPushover.class);

    private static Properties getProperties(PropertiesProvider provider) {
        Properties properties = new Properties();
        // (required) token from pushover.net
        String token = provider.getString("notificator.pushover.token");
        if (token != null) {
            properties.put("notificator.pushover.token", token);
        }
        // (required) user from pushover.net
        String user = provider.getString("notificator.pushover.user");
        if (user != null) {
            properties.put("notificator.pushover.user", user);
        }
        // optional: your user's device name to send the message directly
        // to that device, rather than all of the user's devices (multiple devices may be separated by a comma)
        String device = provider.getString("notificator.pushover.device");
        if (device != null) {
            properties.put("notificator.pushover.device", device);
        }
        // optional: your message's title, otherwise your app's name is used
        String title = provider.getString("notificator.pushover.title");
        if (title != null) {
            properties.put("notificator.pushover.title", title);
        }
        return properties;
    }


    private final String url;
    private String token;
    private String puser;
    private String device;
    private String title;

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

    public NotificatorPushover() {
        // see https://pushover.net/api
        url = "https://api.pushover.net/1/messages.json";
    }

    @Override
    public void sendSync(long userId, Event event, Position position) {

        User user = Context.getPermissionsManager().getUser(userId);

        token = null;
        puser = null;
        device = null;
        title = null;

        Properties properties = null;

        properties = getProperties(new PropertiesProvider(Context.getConfig()));
        if (properties != null) {
            token = properties.getProperty("notificator.pushover.token");
            puser = properties.getProperty("notificator.pushover.user");
            device = properties.getProperty("notificator.pushover.device");
            title = properties.getProperty("notificator.pushover.title");
        }

        properties = getProperties(new PropertiesProvider(user));
        if (properties != null) {
            if (properties.getProperty("notificator.pushover.token") != null) {
                token = properties.getProperty("notificator.pushover.token");
            }
            if (properties.getProperty("notificator.pushover.user") != null) {
                puser = properties.getProperty("notificator.pushover.user");
            }
            if (properties.getProperty("notificator.pushover.device") != null) {
                device = properties.getProperty("notificator.pushover.device");
            }
            if (properties.getProperty("notificator.pushover.title") != null) { 
                title = properties.getProperty("notificator.pushover.title");
            }
        } 
        
        if (token == null) {
            LOGGER.warn("Pushover token not found");
            return;
        }

        if (puser == null) {
            LOGGER.warn("Pushover user not found");
            return;
        }

        if (device == null)
            device = "";

        if (title == null)
            title = "";

        Message message = new Message();
        message.token = token;
        message.user = puser;
        message.device = device;
        message.title = title;
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
