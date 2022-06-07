/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import org.traccar.Context;
import org.traccar.Main;
import org.traccar.database.StatisticsManager;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.NotificationMessage;
import org.traccar.session.cache.CacheManager;

public final class NotificatorSms extends Notificator {

    @Override
    public void sendAsync(User user, Event event, Position position) {
        if (user.getPhone() != null) {
            NotificationMessage shortMessage = NotificationFormatter.formatMessage(
                    Main.getInjector().getInstance(CacheManager.class), user, event, position, "short");
            Main.getInjector().getInstance(StatisticsManager.class).registerSms();
            Context.getSmsManager().sendMessageAsync(user.getPhone(),
                    shortMessage.getBody(), false);
        }
    }

    @Override
    public void sendSync(User user, Event event, Position position) throws MessageException, InterruptedException {
        if (user.getPhone() != null) {
            NotificationMessage shortMessage = NotificationFormatter.formatMessage(
                    Main.getInjector().getInstance(CacheManager.class), user, event, position, "short");
            Main.getInjector().getInstance(StatisticsManager.class).registerSms();
            Context.getSmsManager().sendMessageSync(user.getPhone(),
                    shortMessage.getBody(), false);
        }
    }

}
