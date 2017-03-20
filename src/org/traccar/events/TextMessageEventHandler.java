/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.events;

import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;

public final class TextMessageEventHandler {

    private TextMessageEventHandler() {
    }

    public static void handleTextMessage(String phone, String message) {
        Device device = Context.getDeviceManager().getDeviceByPhone(phone);
        if (device != null && Context.getNotificationManager() != null) {
            Event event = new Event(Event.TYPE_TEXT_MESSAGE, device.getId());
            event.set("message", message);
            Context.getNotificationManager().updateEvent(event, null);
        }
    }

}
