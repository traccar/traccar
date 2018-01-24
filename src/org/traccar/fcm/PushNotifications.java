package org.traccar.fcm;

import de.bytefish.fcmjava.client.FcmClient;
import de.bytefish.fcmjava.client.settings.PropertiesBasedSettings;
import de.bytefish.fcmjava.model.options.FcmMessageOptions;
import de.bytefish.fcmjava.requests.data.DataUnicastMessage;
import de.bytefish.fcmjava.requests.notification.NotificationPayload;
import de.bytefish.fcmjava.responses.FcmMessageResponse;
import de.bytefish.fcmjava.responses.FcmMessageResultItem;

import org.traccar.helper.Log;

import java.time.Duration;
import java.util.Set;

public final class PushNotifications {

    private static PushNotifications instance;

    private FcmClient fcmClient;

    private PushNotifications() {
        fcmClient = new FcmClient(PropertiesBasedSettings.createFromDefault());
    }

    static {
        instance = new PushNotifications();
    }

    private static NotificationPayload buildFCMPayload(String messageTitle, String messageBody) {
        return NotificationPayload.builder()
                                  .setTitle(messageTitle)
                                  .setBody(messageBody)
                                  .build();
    }

    public static PushNotifications getInstance() {
        return instance;
    }

    public void sendEventNotification(Set<String> clientTokens,
                                      String messageTitle,
                                      String messageBody) {

        FcmMessageOptions options = FcmMessageOptions.builder()
                                                     .setTimeToLive(Duration.ofMinutes(2))
                                                     .build();

        for (String clientToken: clientTokens) {

            NotificationPayload payload = buildFCMPayload(messageTitle, messageBody);

            // What's data here?
            DataUnicastMessage message = new DataUnicastMessage(
                    options, clientToken, Math.random() * 1000, payload);

            FcmMessageResponse response = fcmClient.send(message);

            for (FcmMessageResultItem result : response.getResults()) {
                if (result.getErrorCode() != null) {
                    Log.error(String.format(
                            "Sending to %s failed. Error Code %s\n", clientToken, result.getErrorCode()));
                }
            }
        }
    }
}
