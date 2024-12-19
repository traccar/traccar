/*
 * Copyright 2018 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.sms;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.DataConverter;
import org.traccar.notification.MessageException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpSmsClient implements SmsManager {

    private final Client client;
    private final String url;
    private final String authorizationHeader;
    private final String authorization;
    private final String template;
    private final MediaType mediaType;

    public HttpSmsClient(Config config, Client client) {
        this.client = client;
        url = config.getString(Keys.SMS_HTTP_URL);
        authorizationHeader = config.getString(Keys.SMS_HTTP_AUTHORIZATION_HEADER);
        if (config.hasKey(Keys.SMS_HTTP_AUTHORIZATION)) {
            authorization = config.getString(Keys.SMS_HTTP_AUTHORIZATION);
        } else {
            String user = config.getString(Keys.SMS_HTTP_USER);
            String password = config.getString(Keys.SMS_HTTP_PASSWORD);
            if (user != null && password != null) {
                authorization = "Basic "
                        + DataConverter.printBase64((user + ":" + password).getBytes(StandardCharsets.UTF_8));
            } else {
                authorization = null;
            }
        }
        template = config.getString(Keys.SMS_HTTP_TEMPLATE).trim();
        if (template.charAt(0) == '<') {
            mediaType = MediaType.APPLICATION_XML_TYPE;
        } else if (template.charAt(0) == '{' || template.charAt(0) == '[') {
            mediaType = MediaType.APPLICATION_JSON_TYPE;
        } else {
            mediaType = MediaType.APPLICATION_FORM_URLENCODED_TYPE;
        }
    }

    private String prepareValue(String value) throws UnsupportedEncodingException {
        if (mediaType == MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            return new String(JsonStringEncoder.getInstance().quoteAsString(value));
        }
        return value;
    }

    private String preparePayload(String phone, String message) {
        try {
            return template
                    .replace("{phone}", prepareValue(phone))
                    .replace("{message}", prepareValue(message));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Invocation.Builder getRequestBuilder() {
        Invocation.Builder builder = client.target(url).request();
        if (authorization != null) {
            builder = builder.header(authorizationHeader, authorization);
        }
        return builder;
    }

    @Override
    public void sendMessage(String phone, String message, boolean command) throws MessageException {
        try (Response response = getRequestBuilder()
                .post(Entity.entity(preparePayload(phone, message), mediaType))) {
            if (response.getStatus() / 100 != 2) {
                throw new MessageException(response.readEntity(String.class));
            }
        }
    }

}
