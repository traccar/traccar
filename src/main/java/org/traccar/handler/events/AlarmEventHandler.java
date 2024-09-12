/*
 * Copyright 2016 - 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler.events;

import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AlarmEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;
    private final boolean ignoreDuplicates;

    @Inject
    public AlarmEventHandler(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        ignoreDuplicates = config.getBoolean(Keys.EVENT_IGNORE_DUPLICATE_ALERTS);
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        String alarmString = position.getString(Position.KEY_ALARM);
        if (alarmString != null) {
            Set<String> alarms = new HashSet<>(Arrays.asList(alarmString.split(",")));
            if (ignoreDuplicates) {
                Position lastPosition = cacheManager.getPosition(position.getDeviceId());
                if (lastPosition != null) {
                    String lastAlarmString = lastPosition.getString(Position.KEY_ALARM);
                    if (lastAlarmString != null) {
                        Set<String> lastAlarms = new HashSet<>(Arrays.asList(lastAlarmString.split(",")));
                        alarms.removeAll(lastAlarms);
                    }
                }
            }
            for (String alarm : alarms) {
                Event event = new Event(Event.TYPE_ALARM, position);
                event.set(Position.KEY_ALARM, alarm);
                callback.eventDetected(event);
            }
        }
    }

}
