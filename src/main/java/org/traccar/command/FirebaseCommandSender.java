/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.command;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Command;
import org.traccar.model.Device;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Singleton
public class FirebaseCommandSender implements CommandSender {

    private final FirebaseMessaging firebaseMessaging;

    @Inject
    public FirebaseCommandSender(Config config) throws IOException {
        InputStream serviceAccount = new ByteArrayInputStream(
                config.getString(Keys.COMMAND_CLIENT_SERVICE_ACCOUNT).getBytes());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        firebaseMessaging = FirebaseMessaging.getInstance(
                FirebaseApp.initializeApp(options, "client"));
    }

    @Override
    public Collection<String> getSupportedCommands() {
        return List.of(
                Command.TYPE_POSITION_SINGLE,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_POSITION_STOP,
                Command.TYPE_FACTORY_RESET);
    }

    @Override
    public void sendCommand(Device device, Command command) throws Exception {
        if (!device.hasAttribute("notificationTokens")) {
            throw new RuntimeException("Missing device notification tokens");
        }

        List<String> registrationTokens = new ArrayList<>(
                Arrays.asList(device.getString("notificationTokens").split("[, ]")));

        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();

        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder().setContentAvailable(true).build())
                .putHeader("apns-push-type", "background")
                .putHeader("apns-priority", "10")
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .putData("command", command.getType())
                .setAndroidConfig(androidConfig)
                .setApnsConfig(apnsConfig)
                .addAllTokens(registrationTokens)
                .build();

        var result = firebaseMessaging.sendEachForMulticast(message);
        if (result.getFailureCount() > 0) {
            throw result.getResponses().iterator().next().getException();
        }
    }

}
