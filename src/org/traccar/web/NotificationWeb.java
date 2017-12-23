package org.traccar.web;

import org.joda.time.Duration;
import org.traccar.Context;
import org.traccar.fcm.client.FcmClient;
import org.traccar.fcm.core.model.enums.PriorityEnum;
import org.traccar.fcm.core.model.options.FcmMessageOptions;
import org.traccar.fcm.core.requests.notification.NotificationPayload;
import org.traccar.fcm.core.requests.notification.NotificationUnicastMessage;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.PropertiesProvider;

import java.util.Properties;

/**
 * Created by parveenkumaryadav on 24/12/17.
 */
public final class NotificationWeb {

    private NotificationWeb() {
    }

    private static Properties getProperties(PropertiesProvider provider) {
        Properties properties = new Properties();
        String fcmServerKey = provider.getString("fcm.api.key");
        if (fcmServerKey != null) {
            properties.put("fcm.api.key", fcmServerKey);
        }
        return properties;
    }

    public static void sendWebAsync(long userId, Event event, Position position) {
        User user = Context.getPermissionsManager().getUser(userId);

        Properties properties = null;
        if (!Context.getConfig().getBoolean("fcm.ignoreUserConfig")) {
            properties = getProperties(new PropertiesProvider(user));
        }
        if (properties == null || !properties.containsKey("fcm.api.key")) {
            properties = getProperties(new PropertiesProvider(Context.getConfig()));
        }
        if (!properties.containsKey("fcm.api.key")) {
            Log.warning("No FCM configuration found");
            return;
        }

        String fcmToken = user.getString("fcm.token");
        if (fcmToken == null) {
            Log.warning("FCM token not found");
            return;
        }
        try {
            FcmClient client = new FcmClient(properties);
            FcmMessageOptions options = FcmMessageOptions.builder()
                    .setTimeToLive(Duration.standardHours(1))
                    .setPriorityEnum(PriorityEnum.High)
                    .build();


            NotificationPayload notificationPayload = NotificationPayload.builder()
                    .setTitle("Traccar")
                    .setBody(NotificationFormatter.formatWebMessage(userId, event, position))
                    .build();

            NotificationUnicastMessage notificationUnicastMessage =
                    new NotificationUnicastMessage(options, fcmToken, notificationPayload);
            client.send(notificationUnicastMessage);
        } catch (Exception e) {
            Log.warning(e);
        }
    }
}
