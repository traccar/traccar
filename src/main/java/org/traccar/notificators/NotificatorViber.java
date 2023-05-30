/*
 * Copyright 2019 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2021 Rafael Miquelino (rafaelmiquelino@gmail.com)
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
import org.traccar.model.Notification;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;

@Singleton
public class NotificatorViber implements Notificator {

    private final NotificationFormatter notificationFormatter;
    private final Client client;
    private final Config config;

    private final String urlSendText;
    private final String urlSendLocation;
    private final String chatId;
    private final boolean sendLocation;

    public static class TextMessage {
        @JsonProperty("receiver")
        private String chatId;
        @JsonProperty("text")
        private String text;
        @JsonProperty("type")
        private String parseMode = "text";
        @JsonProperty("min_api_version")
        private int minApiVersion = 1;
    }

    public static class LatLng {
        @JsonProperty("lat")
        private double latitude;
        @JsonProperty("lon")
        private double longitude;
    }

    public static class LocationMessage {
        @JsonProperty("receiver")
        private String chatId;
        @JsonProperty("location")
        private LatLng location;
        @JsonProperty("min_api_version")
        private int minApiVersion = 1;
    }

    @Inject
    public NotificatorViber(Config config, NotificationFormatter notificationFormatter, Client client) {
        this.notificationFormatter = notificationFormatter;
        this.client = client;
        this.config = config;
        urlSendText = "https://chatapi.viber.com/pa/send_message";
        urlSendLocation = "https://chatapi.viber.com/pa/send_message";
        chatId = config.getString(Keys.NOTIFICATOR_VIBER_CHAT_ID);
        sendLocation = config.getBoolean(Keys.NOTIFICATOR_VIBER_SEND_LOCATION);
    }

    private LocationMessage createLocationMessage(String messageChatId, Position position) {
        LocationMessage locationMessage = new LocationMessage();
        locationMessage.chatId = messageChatId;
        locationMessage.location = new LatLng();
        locationMessage.location.latitude = position.getLatitude();
        locationMessage.location.longitude = position.getLongitude();
        return locationMessage;
    }

    @Override
    public void send(Notification notification, User user, Event event, Position position) {
        var shortMessage = notificationFormatter.formatMessage(user, event, position, "short");

        TextMessage message = new TextMessage();
        message.chatId = user.getString("viberUid");
        if (message.chatId == null) {
            message.chatId = chatId;
        }
        message.text = shortMessage.getBody();
        client.target(urlSendText).request()
                .header("X-Viber-Auth-Token", config.getString(Keys.NOTIFICATOR_VIBER_KEY))
                .post(Entity.json(message))
                .close();
        if (sendLocation && position != null) {
            client.target(urlSendLocation).request()
                    .header("X-Viber-Auth-Token", config.getString(Keys.NOTIFICATOR_VIBER_KEY))
                    .post(Entity.json(createLocationMessage(message.chatId, position)))
                    .close();
        }
    }

}
