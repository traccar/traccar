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
package org.traccar.notificators;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.notification.NotificationFormatter;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;

public class NotificatorTelegram extends Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorTelegram.class);

    private String urlString = "https://api.telegram.org/bot%s/sendMessage";
    private String apikey;
    private String chatid;

    public static class Message {
        @JsonProperty("chat_id")
        private String chatid;
        @JsonProperty("text")
        private String text;
        @JsonProperty("parse_mode")
        private String parsemode;

    }

    public NotificatorTelegram() {
        apikey = Context.getConfig().getString("notificator.Telegram.apikey");
        chatid = Context.getConfig().getString("notificator.Telegram.chatid");
    }

    @Override
    public void sendSync(long userId, Event event, Position position) {

        Message message = new Message();
        message.chatid = chatid;
        message.parsemode = "html";
        message.text = NotificationFormatter.formatShortMessage(userId, event, position);

        urlString =  String.format(urlString, apikey);
        Context.getClient().target(urlString).request()
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

    @Override
    public void sendAsync(long userId, Event event, Position position) {
        sendSync(userId, event, position);
    }

}
