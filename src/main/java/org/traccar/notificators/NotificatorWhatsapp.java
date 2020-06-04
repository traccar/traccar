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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.notification.NotificationFormatter;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import org.traccar.model.User;

public class NotificatorWhatsapp extends Notificator {

    public static final String WHATSAPP_SUFFIX = "@c.us";
    public static final String WHATSAPP = "whatsapp";
    public static final String WHATSAPP_SILENT = "whatsappSilent";
    public static final String WHATSAPP_SECONDARY = "whatsappSecondary";
    public static final String WHATSAPP_SECONDARY_SILENT = "whatsappSecondarySilent";

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorWhatsapp.class);

    private String url;

    public static class Message {

        @JsonProperty("to")
        private String to;
        @JsonProperty("msg")
        private String text;
        @JsonProperty("latitude")
        private Double latitude;
        @JsonProperty("longitude")
        private Double longitude;

        public void setPosition(Position position) {
            this.latitude = position.getLatitude();
            this.longitude = position.getLongitude();
        }
    }

    public NotificatorWhatsapp() {
        url = Context.getConfig().getString("notificator.whatsapp.url");
    }

    @Override
    public void sendSync(long userId, Event event, Position position) {

        User user = Context.getUsersManager().getById(userId);

        if (user == null) {
            return;
        }

        if (position.getString("alarm") != null && position.getString("alarm").equals(Position.ALARM_SOS)) {
            if (user.getBoolean(WHATSAPP_SILENT) == false) {
                try {
                    user.set(WHATSAPP_SILENT, Boolean.TRUE);
                    if (user.getString(WHATSAPP_SECONDARY) != null && user.getBoolean(WHATSAPP_SECONDARY_SILENT) == true) {
                        user.set(WHATSAPP_SECONDARY_SILENT, Boolean.FALSE);
                    }
                    Context.getUsersManager().updateItem(user);
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(NotificatorWhatsapp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

        String text = NotificationFormatter.formatMessage(userId, event, position, "whatsapp");

        String phone1 = user.getString(WHATSAPP) != null && !user.getBoolean(WHATSAPP_SILENT) ? user.getString(WHATSAPP) + WHATSAPP_SUFFIX : "";
        String phone2 = user.getString(WHATSAPP_SECONDARY) != null && !user.getBoolean(WHATSAPP_SECONDARY_SILENT) ? user.getString(WHATSAPP_SECONDARY) + WHATSAPP_SUFFIX : "";

        if (!phone1.isEmpty()) {
            Message message1 = new Message();
            message1.to = phone1;
            message1.text = text;
            if (event.getType().equalsIgnoreCase(Event.TYPE_IGNITION_OFF)) {
                message1.setPosition(position);
            }
            makeRequst(message1);
        }

        if (!phone2.isEmpty()) {
            Message message2 = new Message();
            message2.to = phone2;
            message2.text = text;
            if (event.getType().equalsIgnoreCase(Event.TYPE_IGNITION_OFF)) {
                message2.setPosition(position);
            }
            makeRequst(message2);
        }

    }

    private void makeRequst(Message message) {
        Context.getClient().target(url).request()
                .async().post(Entity.json(message), new InvocationCallback<Object>() {
                    @Override
                    public void completed(Object o) {
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        LOGGER.warn("Whatsapp API error", throwable);
                    }
                });
    }

    @Override
    public void sendAsync(long userId, Event event, Position position) {
        sendSync(userId, event, position);
    }

}
