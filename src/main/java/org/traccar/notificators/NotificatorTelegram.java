/*
 * Copyright 2019 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.User;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.notification.NotificationFormatter;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;

public class NotificatorTelegram extends Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorTelegram.class);

    private final String urlSendText;
    private final String urlSendLocation;
    private final String chatId;

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

    public NotificatorTelegram() {
        urlSendText = String.format(
                "https://api.telegram.org/bot%s/sendMessage",
                Context.getConfig().getString(Keys.NOTIFICATOR_TELEGRAM_KEY));
        urlSendLocation = String.format(
                "https://api.telegram.org/bot%s/sendLocation",
                Context.getConfig().getString(Keys.NOTIFICATOR_TELEGRAM_KEY));
        chatId = Context.getConfig().getString(Keys.NOTIFICATOR_TELEGRAM_CHAT_ID);
    }

    private void executeRequest(String url, Object message) {
        Context.getClient().target(url).request()
                .async().post(Entity.json(message), new InvocationCallback<Object>() {
            @Override
            public void completed(Object o) {
            }

            @Override
            public void failed(Throwable throwable) {
                LOGGER.warn("Telegram API error", throwable);
            }
        });
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
    public void sendSync(long userId, Event event, Position position) {
        User user = Context.getPermissionsManager().getUser(userId);
        TextMessage message = new TextMessage();
        message.chatId = user.getString("telegramChatId");
        if (message.chatId == null) {
            message.chatId = chatId;
        }
        message.text = NotificationFormatter.formatShortMessage(userId, event, position);
        executeRequest(urlSendText, message);
        if (position != null) {
            executeRequest(urlSendLocation, createLocationMessage(message.chatId, position));
        }
    }

    @Override
    public void sendAsync(long userId, Event event, Position position) {
        sendSync(userId, event, position);
    }

}
