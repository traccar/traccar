/*
 * Copyright 2019 - 2024 Anton Tananaev (anton@traccar.org)
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
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

@Singleton
public class NotificatorTelegram extends Notificator {

    private final Client client;
    private final ObjectMapper objectMapper;
    private final Proxy proxy;
    private final Authenticator authenticator;

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
    public NotificatorTelegram(Config config, NotificationFormatter notificationFormatter,
            Client client, ObjectMapper objectMapper) {
        super(notificationFormatter);
        this.client = client;
        this.objectMapper = objectMapper;
        urlSendText = String.format(
                "https://api.telegram.org/bot%s/sendMessage", config.getString(Keys.NOTIFICATOR_TELEGRAM_KEY));
        urlSendLocation = String.format(
                "https://api.telegram.org/bot%s/sendLocation", config.getString(Keys.NOTIFICATOR_TELEGRAM_KEY));
        chatId = config.getString(Keys.NOTIFICATOR_TELEGRAM_CHAT_ID);
        sendLocation = config.getBoolean(Keys.NOTIFICATOR_TELEGRAM_SEND_LOCATION);

        String proxyHost = config.getString(Keys.NOTIFICATOR_TELEGRAM_PROXY_HOST);
        if (proxyHost != null) {
            int proxyPort = config.getInteger(Keys.NOTIFICATOR_TELEGRAM_PROXY_PORT);
            boolean isSocks = "socks5".equalsIgnoreCase(config.getString(Keys.NOTIFICATOR_TELEGRAM_PROXY_TYPE));
            proxy = new Proxy(
                    isSocks ? Proxy.Type.SOCKS : Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, proxyPort));
            String proxyUser = config.getString(Keys.NOTIFICATOR_TELEGRAM_PROXY_USER);
            if (proxyUser != null) {
                String proxyPassword = config.getString(Keys.NOTIFICATOR_TELEGRAM_PROXY_PASSWORD);
                char[] password = proxyPassword != null ? proxyPassword.toCharArray() : new char[0];
                authenticator = new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUser, password);
                    }
                };
            } else {
                authenticator = null;
            }
        } else {
            proxy = null;
            authenticator = null;
        }
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

    private void postWithProxy(String url, Object payload) throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(payload);
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection(proxy);
        if (authenticator != null) {
            connection.setAuthenticator(authenticator);
        }
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body);
        }
        try (InputStream stream = connection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST
                ? connection.getErrorStream() : connection.getInputStream()) {
            // Trigger the request and close the response stream.
        } finally {
            connection.disconnect();
        }
    }

    private void post(String url, Object payload) throws IOException {
        if (proxy != null) {
            postWithProxy(url, payload);
        } else {
            client.target(url).request().post(Entity.json(payload)).close();
        }
    }

    @Override
    public void send(User user, NotificationMessage shortMessage, Event event, Position position) {

        TextMessage message = new TextMessage();
        message.chatId = user.getString("telegramChatId");
        if (message.chatId == null) {
            message.chatId = chatId;
        }
        message.text = shortMessage.digest();

        try {
            post(urlSendText, message);
            if (sendLocation && position != null) {
                post(urlSendLocation, createLocationMessage(message.chatId, position));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
