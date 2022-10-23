/*
 * Copyright 2020 Francesco Rega (francesco@francescorega.eu)
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

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public final class NotificatorMqtt implements Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorMqtt.class);
    private final NotificationFormatter notificationFormatter;
    private final Config config;

    // Initialize connection variables
    private String mqttHost;
    private String mqttUser;
    private String mqttPass;
    private String clientId;
    private String qos;
    private String cleanSession;
    private String retain;

    @Inject
    public NotificatorMqtt(Config config, NotificationFormatter notificationFormatter) {

        this.notificationFormatter = notificationFormatter;
        this.config = config;

        // Read connection configuration values
        mqttHost = (StringUtils.isNotBlank(config.getString("notificator.mqtt.uri")))
                ? config.getString("notificator.mqtt.uri")
                : "tcp://127.0.0.1:1883";
        mqttUser = config.getString("notificator.mqtt.username");
        mqttPass = config.getString("notificator.mqtt.password");
        clientId = (StringUtils.isNotBlank(config.getString("notificator.mqtt.clientid")))
                ? config.getString("notificator.mqtt.clientid")
                : MqttClient.generateClientId();
        qos = (StringUtils.isNotBlank(config.getString("notificator.mqtt.qos")))
                ? config.getString("notificator.mqtt.qos")
                : "0";
        cleanSession = (StringUtils.isNotBlank(config.getString("notificator.mqtt.cleansession")))
                ? config.getString("notificator.mqtt.cleansession")
                : "true";
        retain = (StringUtils.isNotBlank(config.getString("notificator.mqtt.retain")))
                ? config.getString("notificator.mqtt.retain")
                : "false";
    }

    @Override
    public void send(User user, Event event, Position position) {

        try {

            // Create placeholders and substitutors map
            Map<String, String> values = new HashMap<String, String>();
            //values.put("U", String.valueOf(userId)); //FixMe
            values.put("E", event.getType());
            values.put("D", String.valueOf(event.getDeviceId()));
            values.put("G", String.valueOf(event.getGeofenceId()));
            values.put("P", String.valueOf(event.getPositionId()));
            values.put("M", notificationFormatter.formatMessage(user, event, position, "short").getBody());
            StrSubstitutor sub = new StrSubstitutor(values, "%", "%");

            // Read configured topic to publish to, if any, or set default
            String topic = (StringUtils.isNotBlank(config.getString("notificator.mqtt.topic")))
                    ? config.getString("notificator.mqtt.topic")
                    : "/Traccar/Notification/" + event.getType();

            // Read configured payload to publish, if any, or set default
            String payload = (StringUtils.isNotBlank(config.getString("notificator.mqtt.payload")))
                    ? config.getString("notificator.mqtt.payload")
                    : notificationFormatter.formatMessage(user, event, position, "short").getBody();

            // Replace placeholders with real values
            topic = sub.replace(topic);
            payload = sub.replace(payload);

            // Create MQTT client
            IMqttClient client = new MqttClient(mqttHost, clientId);

            // Set connection options and connect
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(Boolean.parseBoolean(cleanSession));
            if ((StringUtils.isNotBlank(mqttUser)) && (StringUtils.isNotBlank(mqttPass))) {
                connOpts.setUserName(mqttUser);
                connOpts.setPassword(mqttPass.toCharArray());
            }
            client.connect(connOpts);

            // Create and publish message
            MqttMessage msg = new MqttMessage();
            msg.setPayload(payload.getBytes());
            msg.setQos(Integer.parseInt(qos));
            msg.setRetained(Boolean.parseBoolean(retain));
            client.publish(topic, msg);

            // Disconnect from the broker
            client.disconnect();

        } catch (MqttSecurityException e) {
            LOGGER.warn("MqttSecurityException", e);
        } catch (MqttException e) {
            LOGGER.warn("MqttException", e);
        }

    }

}
