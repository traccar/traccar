/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import jakarta.ws.rs.core.Response;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.NotificationMessage;

import java.util.List;

@Singleton
public class NotificatorWhatsapp extends Notificator {

    private final Client client;
    private final String token;
    private final String phoneNumberId;
    private final String templateName;
    private final String templateLanguage;

    public static class Language {
        @JsonProperty("code")
        private String code;
    }

    public static class Parameter {
        @JsonProperty("type")
        private String type = "text";
        @JsonProperty("text")
        private String text;
    }

    public static class Component {
        @JsonProperty("type")
        private String type = "body";
        @JsonProperty("parameters")
        private List<Parameter> parameters;
    }

    public static class Template {
        @JsonProperty("name")
        private String name;
        @JsonProperty("language")
        private Language language = new Language();
        @JsonProperty("components")
        private List<Component> components;
    }

    public static class Message {
        @JsonProperty("messaging_product")
        private String messagingProduct = "whatsapp";
        @JsonProperty("to")
        private String to;
        @JsonProperty("type")
        private String type = "template";
        @JsonProperty("template")
        private Template template = new Template();
    }

    @Inject
    public NotificatorWhatsapp(Config config, NotificationFormatter notificationFormatter, Client client) {
        super(notificationFormatter);
        this.client = client;
        token = config.getString(Keys.NOTIFICATOR_WHATSAPP_TOKEN);
        phoneNumberId = config.getString(Keys.NOTIFICATOR_WHATSAPP_PHONE_NUMBER_ID);
        templateName = config.getString(Keys.NOTIFICATOR_WHATSAPP_TEMPLATE_NAME);
        templateLanguage = config.getString(Keys.NOTIFICATOR_WHATSAPP_TEMPLATE_LANGUAGE);
    }

    @Override
    public void send(User user, NotificationMessage shortMessage, Event event, Position position)
            throws MessageException {
        String recipient = user.getPhone();
        if (recipient == null || recipient.isBlank()) {
            return;
        }

        if (token == null || phoneNumberId == null || templateName == null) {
            throw new MessageException("Missing WhatsApp configuration");
        }

        Parameter parameter = new Parameter();
        parameter.text = shortMessage.digest();
        Component component = new Component();
        component.parameters = List.of(parameter);

        Message message = new Message();
        message.to = recipient;
        message.template.name = templateName;
        message.template.language.code = templateLanguage;
        message.template.components = List.of(component);

        try (Response response = client.target(String.format(
                "https://graph.facebook.com/v22.0/%s/messages", phoneNumberId))
                .request().header("Authorization", "Bearer " + token).post(Entity.json(message))) {
            if (response.getStatus() / 100 != 2) {
                throw new MessageException(response.readEntity(String.class));
            }
        }
    }

}
