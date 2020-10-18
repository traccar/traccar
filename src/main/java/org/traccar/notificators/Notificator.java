/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.notification.MessageException;

public abstract class Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Notificator.class);

    public void sendAsync(final long userId, final Event event, final Position position) {
        new Thread(() -> {
            try {
                sendSync(userId, event, position);
            } catch (MessageException | InterruptedException error) {
                LOGGER.warn("Event send error", error);
            }
        }).start();
    }

    public abstract void sendSync(long userId, Event event, Position position)
        throws MessageException, InterruptedException;

}
