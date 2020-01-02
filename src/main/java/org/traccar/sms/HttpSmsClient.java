/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.api.SecurityRequestFilter;
import org.traccar.helper.DataConverter;
import org.traccar.notification.MessageException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpSmsClient implements SmsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSmsClient.class);

    private String url;
    private String authorizationHeader;
    private String authorization;
    private String template;
    private boolean encode;
    private MediaType mediaType;

    public HttpSmsClient() {
        url = Context.getConfig().getString("sms.http.url");
        authorizationHeader = Context.getConfig().getString("sms.http.authorizationHeader",
                SecurityRequestFilter.AUTHORIZATION_HEADER);
        authorization = Context.getConfig().getString("sms.http.authorization");
        if (authorization == null) {
            String user = Context.getConfig().getString("sms.http.user");
            String password = Context.getConfig().getString("sms.http.password");
            authorization = "Basic "
                    + DataConverter.printBase64((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        }
        template = Context.getConfig().getString("sms.http.template").trim();
        if (template.charAt(0) == '{' || template.charAt(0) == '[') {
            encode = false;
            mediaType = MediaType.APPLICATION_JSON_TYPE;
        } else {
            encode = true;
            mediaType = MediaType.APPLICATION_FORM_URLENCODED_TYPE;
        }
    }

    private String prepareValue(String value) throws UnsupportedEncodingException {
        return encode ? URLEncoder.encode(value, StandardCharsets.UTF_8.name()) : value;
    }

    private String preparePayload(String destAddress, String message) {
        try {
            return template
                    .replace("{phone}", prepareValue(destAddress))
                    .replace("{message}", prepareValue(message.trim()));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Invocation.Builder getRequestBuilder() {
        return Context.getClient().target(url).request()
                .header(authorizationHeader, authorization);
    }

    @Override
    public void sendMessageSync(String destAddress, String message, boolean command) throws MessageException {
        Response response = getRequestBuilder().post(Entity.entity(preparePayload(destAddress, message), mediaType));
        if (response.getStatus() / 100 != 2) {
            throw new MessageException(response.readEntity(String.class));
        }
    }

    @Override
    public void sendMessageAsync(final String destAddress, final String message, final boolean command) {
        getRequestBuilder().async().post(
                Entity.entity(preparePayload(destAddress, message), mediaType), new InvocationCallback<String>() {
            @Override
            public void completed(String s) {
            }

            @Override
            public void failed(Throwable throwable) {
                LOGGER.warn("SMS send failed", throwable);
            }
        });
    }

}
