/*
 * Copyright 2019 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Position;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class TimeHandler extends BasePositionHandler {

    private static final long ROLLOVER_CYCLE = 1024 * Duration.ofDays(7).toMillis();
    private static final long ROLLOVER_THRESHOLD = ROLLOVER_CYCLE - Duration.ofDays(90).toMillis();

    private final String overrideType;
    private final Set<String> overrideProtocols;

    @Inject
    public TimeHandler(Config config) {

        overrideType = config.getString(Keys.TIME_OVERRIDE);
        String protocolList = config.getString(Keys.TIME_PROTOCOLS);
        if (protocolList != null) {
            overrideProtocols = new HashSet<>(Arrays.asList(protocolList.split("[, ]")));
        } else {
            overrideProtocols = null;
        }
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        handleRollover(position);
        handleOverride(position);
        callback.processed(false);
    }

    private void handleRollover(Position position) {
        long currentTime = System.currentTimeMillis();
        position.setDeviceTime(adjustRollover(currentTime, position.getDeviceTime()));
        position.setFixTime(adjustRollover(currentTime, position.getFixTime()));
    }

    public static Date adjustRollover(long currentTime, Date time) {
        long positionTime = time.getTime();
        while (positionTime > OutdatedHandler.GPS_EPOCH && currentTime - positionTime > ROLLOVER_THRESHOLD) {
            positionTime += ROLLOVER_CYCLE;
        }
        return positionTime == time.getTime() ? time : new Date(positionTime);
    }

    private void handleOverride(Position position) {
        if (overrideType == null) {
            return;
        }
        if (overrideProtocols != null && !overrideProtocols.contains(position.getProtocol())) {
            return;
        }
        switch (overrideType) {
            case "serverTime":
                position.setDeviceTime(position.getServerTime());
                position.setFixTime(position.getServerTime());
                break;
            case "deviceTime":
            default:
                position.setFixTime(position.getDeviceTime());
                break;
        }
    }

}
