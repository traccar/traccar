package org.traccar.notificators;

import com.google.gson.Gson;
import net.minidev.json.parser.JSONParser;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Notification;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Entity;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Singleton
public class NotificatorMqtt implements Notificator{
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorMqtt.class);
    private final IMqttClient publisher;
    private final MqttConnectOptions options = new MqttConnectOptions();

    private final NotificationFormatter notificationFormatter;
    @Inject
    public NotificatorMqtt(Config config, NotificationFormatter notificationFormatter) throws MqttException {
        this.notificationFormatter = notificationFormatter;
        publisher = new MqttClient(config.getString(Keys.NOTIFICATOR_MQTT_SERVER_URL)
                ,config.getString(Keys.NOTIFICATOR_MQTT_CLIENT_ID));
        if(config.getString(Keys.NOTIFICATOR_MQTT_USERNAME) != null) {
            options.setUserName(config.getString(Keys.NOTIFICATOR_MQTT_USERNAME));
            options.setPassword(config.getString(Keys.NOTIFICATOR_MQTT_PASSWORD).toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        publisher.connect(options);
    }

    @Override
    public void send(Notification notification, User user, Event event, Position position) throws MessageException {
        if ( !publisher.isConnected()) {
            try {
                publisher.connect(options);
            } catch (MqttException e) {
                LOGGER.warn("Mqtt error", e);
            }
        }
        var shortMessage = notificationFormatter.formatMessage(user, event, position, "short");
        MqttMessage message = new MqttMessage();
        message.setPayload(new Gson().toJson(shortMessage).getBytes());
        message.setQos(0);
        try {
            publisher.publish(String.valueOf(event.getDeviceId()),message);
        } catch (MqttException e) {
            LOGGER.warn("Mqtt error", e);
        }
    }
}
