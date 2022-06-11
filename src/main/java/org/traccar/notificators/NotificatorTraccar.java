/*
 * Copyright 2020 - 2022 Anton Tananaev (anton@traccar.org)
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

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.notification.NotificationFormatter;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

public class NotificatorTraccar extends NotificatorFirebase {

    @Inject
    public NotificatorTraccar(Config config, NotificationFormatter notificationFormatter, Client client) {
        super(
                notificationFormatter, client, "https://www.traccar.org/push/",
                config.getString(Keys.NOTIFICATOR_TRACCAR_KEY));
    }

}
