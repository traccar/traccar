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

import org.traccar.database.StatisticsManager;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;
import org.traccar.sms.SmsManager;

import javax.inject.Inject;

public class NotificatorSms implements Notificator {

    private final SmsManager smsManager;
    private final NotificationFormatter notificationFormatter;
    private final StatisticsManager statisticsManager;

    @Inject
    public NotificatorSms(
            SmsManager smsManager, NotificationFormatter notificationFormatter, StatisticsManager statisticsManager) {
        this.smsManager = smsManager;
        this.notificationFormatter = notificationFormatter;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void send(User user, Event event, Position position) throws MessageException, InterruptedException {
        if (user.getPhone() != null) {
            var shortMessage = notificationFormatter.formatMessage(user, event, position, "short");
            statisticsManager.registerSms();
            smsManager.sendMessage(user.getPhone(), shortMessage.getBody(), false);
        }
    }

}
