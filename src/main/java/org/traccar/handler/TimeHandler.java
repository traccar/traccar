/*
 * Copyright 2019 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class TimeHandler extends BasePositionHandler {

    private final boolean useServerTime;
    private final Set<String> protocols;
    private final CacheManager cacheManager;

    @Inject
    public TimeHandler(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        useServerTime = config.getString(Keys.TIME_OVERRIDE).equalsIgnoreCase("serverTime");
        String protocolList = config.getString(Keys.TIME_PROTOCOLS);
        if (protocolList != null) {
            protocols = new HashSet<>(Arrays.asList(protocolList.split("[, ]")));
        } else {
            protocols = null;
        }
    }

    @Override
    public void onPosition(Position position, Callback callback) {

        if (protocols == null || protocols.contains(position.getProtocol())) {
            if (useServerTime) {
                position.setDeviceTime(position.getServerTime());
                position.setFixTime(position.getServerTime());
            } else {
                position.setFixTime(position.getDeviceTime());
            }
        }

        // Apply time offset from device/group/global configuration
        Integer offset = AttributeUtil.lookup(cacheManager, Keys.TIME_OFFSET, position.getDeviceId());
        if (offset != null && offset != 0) {
            // Offset is in seconds, convert to milliseconds
            long offsetMillis = offset * 1000L;

            if (position.getDeviceTime() != null) {
                position.setDeviceTime(new Date(position.getDeviceTime().getTime() + offsetMillis));
            }
            if (position.getFixTime() != null) {
                position.setFixTime(new Date(position.getFixTime().getTime() + offsetMillis));
            }
        }

        callback.processed(false);
    }

}
