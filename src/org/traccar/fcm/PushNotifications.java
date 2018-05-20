package org.traccar.fcm;

import org.traccar.helper.Log;

import java.util.Set;

public final class PushNotifications {

    private static PushNotifications instance;

    private PushNotifications() {
    }

    static {
        instance = new PushNotifications();
    }

    public static PushNotifications getInstance() {
        return instance;
    }

    public void sendEventNotification(Set<String> clientTokens,
                                      String messageTitle,
                                      String messageBody,
                                      int ttl) {
        try {
            for (String clientToken : clientTokens) {
                PushNotificationHelper.sendPushNotification(clientToken, messageTitle, messageBody, ttl);
            }
        } catch (Exception e) {
            Log.error("ERROR ON SEND: " + e.getMessage());
        }
    }
}
