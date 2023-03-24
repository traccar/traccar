/*
 * Copyright 2018 - 2022 Anton Tananaev (anton@traccar.org)
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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Singleton
public class NotificatorFirebase implements Notificator {

    private final NotificationFormatter notificationFormatter;

    @Inject
    public NotificatorFirebase(Config config, NotificationFormatter notificationFormatter) throws IOException {

        this.notificationFormatter = notificationFormatter;

        InputStream serviceAccount = new ByteArrayInputStream(
                config.getString(Keys.NOTIFICATOR_FIREBASE_SERVICE_ACCOUNT).getBytes());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
    }

    @Override
    public void send(User user, Event event, Position position) throws MessageException {
        if (user.hasAttribute("notificationTokens")) {

            var shortMessage = notificationFormatter.formatMessage(user, event, position, "short");

            String[] registrationTokens = user.getString("notificationTokens").split("[, ]");

            List<String> bodyArguments = notificationFormatter.getEventValues(user, event, position);

            String notificationTitle = event.getType() + "_title";
            String notificationBody = event.getType();

            for (String token :registrationTokens) {
                Message message = Message.builder()
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(shortMessage.getSubject())
                                .setBody(shortMessage.getBody())
                                .build())
                        .setAndroidConfig(AndroidConfig.builder()
                                .setNotification(AndroidNotification.builder()
                                        .setTitleLocalizationKey(notificationTitle)
                                        .setBodyLocalizationKey(notificationBody)
                                        .addAllBodyLocalizationArgs(bodyArguments)
                                        .setSound("default")
                                        .build())
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder()
                                        .setAlert(ApsAlert.builder()
                                                .setTitleLocalizationKey(notificationTitle)
                                                .setSubtitleLocalizationKey(notificationBody)
                                                .addAllSubtitleLocArgs(bodyArguments)
                                                .build())
                                        .setSound("default")
                                        .build())
                                .build())
                        .setTopic(event.getType())
                        .setToken(token)
                        .putData("eventId", String.valueOf(event.getId()))
                        .putData("event", event.toString())
                        .putData("user", user.toString())
                        .putData("position", position.toString())
                        .build();

                try {

                    var result = FirebaseMessaging.getInstance().send(message);
                    if (result == null || result.isEmpty() || result.isBlank()) {
                        throw new MessageException("Failed to send push notification to device : " + message);
                    }

                } catch (FirebaseMessagingException e) {
                    throw new MessageException(e);
                }
            }
        }
    }

}
