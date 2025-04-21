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
package org.traccar.push;

import com.google.firebase.messaging.MulticastMessage;
import org.traccar.model.Command;
import org.traccar.model.Device;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PushCommandManager {

    private final FirebaseClient firebaseClient;

    public PushCommandManager(FirebaseClient firebaseClient) {
        this.firebaseClient = firebaseClient;
    }

    public void sendCommand(Device device, Command command) throws Exception {
        if (!device.hasAttribute("notificationTokens")) {
            throw new RuntimeException("Missing device notification tokens");
        }

        List<String> registrationTokens = new ArrayList<>(
                Arrays.asList(device.getString("notificationTokens").split("[, ]")));

        MulticastMessage message = MulticastMessage.builder()
                .putData("command", command.getType())
                .addAllTokens(registrationTokens)
                .build();

        var result = firebaseClient.getInstance().sendEachForMulticast(message);
        if (result.getFailureCount() > 0) {
            throw new RuntimeException("Failed to send device push");
        }
    }

}
