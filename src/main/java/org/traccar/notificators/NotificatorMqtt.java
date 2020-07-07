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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.notification.NotificationFormatter;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import java.util.UUID;

public class NotificatorMqtt extends Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorMqtt.class);

    private String mqttHost;
    private String mqttUser;
    private String mqttPass;
    private String clientId;

    public NotificatorMqtt() {
        mqttHost = Context.getConfig().getString("notificator.mqtt.hostname");
        mqttUser = Context.getConfig().getString("notificator.mqtt.username");
        mqttPass = Context.getConfig().getString("notificator.mqtt.password");
        clientId = (StringUtils.isNotBlank(Context.getConfig().getString("notificator.mqtt.clientid"))) ? Context.getConfig().getString("notificator.mqtt.clientid") : MqttClient.generateClientId();
    }

    @Override
    public void sendSync(long userId, Event event, Position position) {

        try {

            IMqttClient client = new MqttClient(mqttHost, clientId);

            MqttConnectOptions connOpts = setUpConnectionOptions(mqttUser, mqttPass);
            client.connect(connOpts);

            MqttMessage msg = new MqttMessage();
            msg.setPayload(NotificationFormatter.formatShortMessage(userId, event, position).getBytes());
            msg.setQos(0);
            msg.setRetained(true);
            client.publish("/Traccar/Notification",msg);


            /*
            Message message = new Message();
            message.chatId = chatId;
            message.text = NotificationFormatter.formatShortMessage(userId, event, position);

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
            */


        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendAsync(long userId, Event event, Position position) {
        sendSync(userId, event, position);
    }

    private static MqttConnectOptions setUpConnectionOptions(String username, String password) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setUserName(username);
        connOpts.setPassword(password.toCharArray());
        return connOpts;
    }

}
