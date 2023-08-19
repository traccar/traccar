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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;

@Singleton
public class NotificatorTelegram implements Notificator {

    private final NotificationFormatter notificationFormatter;
    private final Client client;

    private final String urlSendText;
    private final String urlSendLocation;
    private final String chatId;
    private final boolean sendLocation;

    public static class TextMessage {
        @JsonProperty("chat_id")
        private String chatId;
        @JsonProperty("text")
        private String text;
        @JsonProperty("parse_mode")
        private String parseMode = "html";
    }

    public static class LocationMessage {
        @JsonProperty("chat_id")
        private String chatId;
        @JsonProperty("latitude")
        private double latitude;
        @JsonProperty("longitude")
        private double longitude;
        @JsonProperty("horizontal_accuracy")
        private double accuracy;
        @JsonProperty("bearing")
        private int bearing;
    }

    @Inject
    public NotificatorTelegram(Config config, NotificationFormatter notificationFormatter, Client client) {
        this.notificationFormatter = notificationFormatter;
        this.client = client;
        urlSendText = String.format(
                "https://api.telegram.org/bot%s/sendMessage", config.getString(Keys.NOTIFICATOR_TELEGRAM_KEY));
        urlSendLocation = String.format(
                "https://api.telegram.org/bot%s/sendLocation", config.getString(Keys.NOTIFICATOR_TELEGRAM_KEY));
        chatId = config.getString(Keys.NOTIFICATOR_TELEGRAM_CHAT_ID);
        sendLocation = config.getBoolean(Keys.NOTIFICATOR_TELEGRAM_SEND_LOCATION);
    }

    private LocationMessage createLocationMessage(String messageChatId, Position position) {
        LocationMessage locationMessage = new LocationMessage();
        locationMessage.chatId = messageChatId;
        locationMessage.latitude = position.getLatitude();
        locationMessage.longitude = position.getLongitude();
        locationMessage.bearing = (int) Math.ceil(position.getCourse());
        locationMessage.accuracy = position.getAccuracy();
        return locationMessage;
    }

    @Override
    public void send(Notification notification, User user, Event event, Position position) {
        var shortMessage = notificationFormatter.formatMessage(user, event, position, "short");

        TextMessage message = new TextMessage();
        message.chatId = user.getString("telegramChatId");
        if (message.chatId == null) {
            message.chatId = chatId;
        }
        message.text = shortMessage.getBody();
        client.target(urlSendText).request().post(Entity.json(message)).close();
        if (sendLocation && position != null) {
            client.target(urlSendLocation).request().post(
                    Entity.json(createLocationMessage(message.chatId, position))).close();
        }
    }

}
