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
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;

import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import java.util.HashMap;

public class NotificatorTelegram extends Notificator {
    private static final String TELEGRAM_API_PREFIX = "https://api.telegram.org/bot%s/%s";
    private static final long HOUR_TIME_FACTOR = 1000 * 60 * 60; // mSec to hour factor
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorTelegram.class);

    private String sendMessageUrl;
    private String sendContactUrl;
    private String chatId;
    private TelegramCache cache = new TelegramCache();

    public static class TextMessage {
        public TextMessage(String chatId, String text) {
            this.chatId = chatId;
            this.text = text;
        }
        @JsonProperty("chat_id")
        private String chatId;
        @JsonProperty("text")
        private String text;
        @JsonProperty("parse_mode")
        private String parseMode = "html";
    }

    public static class ContactMessage {
        public ContactMessage(String chatId, String phoneNumber) {
            this.chatId = chatId;
            this.phoneNumber = phoneNumber;
        }
        @JsonProperty("chat_id")
        private String chatId;
        @JsonProperty("phone_number")
        private String phoneNumber;
        @JsonProperty("first_name")
        private String firstName = "dummy";
    }

    public static class TelegramCache {
        private class CachePayload {
            CachePayload(String chatId) {
                this.chatId = chatId;
                timestamp = System.currentTimeMillis();
            }
            private String chatId;
            private long timestamp;
        }

        private HashMap<String, CachePayload> cache = new HashMap<>();
        private long retention;

        public TelegramCache() {
            retention = Context.getConfig().getLong("notificator.telegram.cacheRetention", 0) * HOUR_TIME_FACTOR;
        }

        private Boolean isValid(long timestamp) {
            long currTime = System.currentTimeMillis();
            return retention < 0 || ((currTime - timestamp) <= retention);
        }

        public String getChatId(String phoneNumber) {
            CachePayload payload = cache.get(phoneNumber);
            if (payload != null && isValid(payload.timestamp)) {
                return payload.chatId;
            }
            return null;
        }

        public void storeCache(String phoneNumber, String chatId) {
            cache.put(phoneNumber, new CachePayload(chatId));
        }
    }

    public NotificatorTelegram() {
        String botKey = Context.getConfig().getString("notificator.telegram.key");
        sendMessageUrl = String.format(TELEGRAM_API_PREFIX, botKey, "sendMessage");
        sendContactUrl = String.format(TELEGRAM_API_PREFIX, botKey, "sendContact");
        chatId = Context.getConfig().getString("notificator.telegram.chatId");
    }

    private void sendTextMessage(long userId, Event event, Position position, String userChatId) {
        TextMessage message = new TextMessage(
                userChatId, NotificationFormatter.formatShortMessage(userId, event, position));

        Context.getClient().target(sendMessageUrl).request()
                .async().post(Entity.json(message), new InvocationCallback<Object>() {
            @Override
            public void completed(Object o) {
            }

            @Override
            public void failed(Throwable throwable) {
                LOGGER.warn("Telegram API error - send message failed", throwable);
            }
        });
    }

    @Override
    public void sendSync(long userId, Event event, Position position) {

        final User user = Context.getPermissionsManager().getUser(userId);
        Boolean checkTelegramId = user.getAttributes().containsKey("notificationTelegramChatId");
        if (user.getPhone() != null || checkTelegramId) {
            String userChatId;
            if (checkTelegramId) {
                userChatId = user.getString("notificationTelegramChatId");
            } else {
                userChatId = cache.getChatId(user.getPhone());
            }
            if (userChatId != null) {
                sendTextMessage(userId, event, position, userChatId);
            } else {
                ContactMessage message = new ContactMessage(chatId, user.getPhone());
                Context.getClient().target(sendContactUrl).request()
                        .async().post(Entity.json(message), new InvocationCallback<JsonObject>() {
                    @Override
                    public void completed(JsonObject json) {
                        JsonObject result = json.getJsonObject("result");
                        if (json.getBoolean("ok")) {
                            JsonObject contact = result.getJsonObject("contact");
                            String userChatId = contact.getJsonNumber("user_id").toString();

                            cache.storeCache(user.getPhone(), userChatId);
                            sendTextMessage(userId, event, position, userChatId);
                        } else {
                            LOGGER.warn("Telegram notificator: contact retrieval failed");
                        }
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        LOGGER.warn("Telegram API error - user " + user.getName() + "chatId retrieval failed",
                                throwable);
                    }
                });
            }
        } else {
            LOGGER.warn("Telegram notificator: Couldn't find phone number for user " + user.getName() + " - " + user.getPhone());
        }
    }

    @Override
    public void sendAsync(long userId, Event event, Position position) {
        sendSync(userId, event, position);
    }
}
