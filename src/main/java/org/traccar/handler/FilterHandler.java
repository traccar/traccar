/*
 * Copyright 2014 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.StatisticsManager;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.LinkedList;
import java.util.List;

public class FilterHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterHandler.class);

    private final boolean filterInvalid;
    private final boolean filterZero;
    private final boolean filterDuplicate;
    private final boolean filterOutdated;
    private final long filterFuture;
    private final long filterPast;
    private final boolean filterApproximate;
    private final int filterAccuracy;
    private final boolean filterStatic;
    private final int filterDistance;
    private final int filterMaxSpeed;
    private final long filterMinPeriod;
    private final int filterDailyLimit;
    private final long filterDailyLimitInterval;
    private final long skipLimit;
    private final boolean skipAttributes;

    private final CacheManager cacheManager;
    private final StatisticsManager statisticsManager;

    @Inject
    public FilterHandler(
            Config config, CacheManager cacheManager, StatisticsManager statisticsManager) {
        filterInvalid = config.getBoolean(Keys.FILTER_INVALID);
        filterZero = config.getBoolean(Keys.FILTER_ZERO);
        filterDuplicate = config.getBoolean(Keys.FILTER_DUPLICATE);
        filterOutdated = config.getBoolean(Keys.FILTER_OUTDATED);
        filterFuture = config.getLong(Keys.FILTER_FUTURE) * 1000;
        filterPast = config.getLong(Keys.FILTER_PAST) * 1000;
        filterAccuracy = config.getInteger(Keys.FILTER_ACCURACY);
        filterApproximate = config.getBoolean(Keys.FILTER_APPROXIMATE);
        filterStatic = config.getBoolean(Keys.FILTER_STATIC);
        filterDistance = config.getInteger(Keys.FILTER_DISTANCE);
        filterMaxSpeed = config.getInteger(Keys.FILTER_MAX_SPEED);
        filterMinPeriod = config.getInteger(Keys.FILTER_MIN_PERIOD) * 1000L;
        filterDailyLimit = config.getInteger(Keys.FILTER_DAILY_LIMIT);
        filterDailyLimitInterval = config.getInteger(Keys.FILTER_DAILY_LIMIT_INTERVAL) * 1000L;
        skipLimit = config.getLong(Keys.FILTER_SKIP_LIMIT) * 1000;
        skipAttributes = config.getBoolean(Keys.FILTER_SKIP_ATTRIBUTES_ENABLE);
        this.cacheManager = cacheManager;
        this.statisticsManager = statisticsManager;
    }

    private boolean filterInvalid(Position position) {
        return filterInvalid && (!position.getValid()
                || position.getLatitude() > 90 || position.getLongitude() > 180
                || position.getLatitude() < -90 || position.getLongitude() < -180);
    }

    private boolean filterZero(Position position) {
        return filterZero && position.getLatitude() == 0.0 && position.getLongitude() == 0.0;
    }

    private boolean filterDuplicate(Position position, Position last) {
        if (filterDuplicate && last != null && position.getFixTime().equals(last.getFixTime())) {
            for (String key : position.getAttributes().keySet()) {
                if (!last.hasAttribute(key)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean filterOutdated(Position position) {
        return filterOutdated && position.getOutdated();
    }

    private boolean filterFuture(Position position) {
        return filterFuture != 0 && position.getFixTime().getTime() > System.currentTimeMillis() + filterFuture;
    }

    private boolean filterPast(Position position) {
        return filterPast != 0 && position.getFixTime().getTime() < System.currentTimeMillis() - filterPast;
    }

    private boolean filterAccuracy(Position position) {
        return filterAccuracy != 0 && position.getAccuracy() > filterAccuracy;
    }

    private boolean filterApproximate(Position position) {
        return filterApproximate && position.getBoolean(Position.KEY_APPROXIMATE);
    }

    private boolean filterStatic(Position position) {
        return filterStatic && position.getSpeed() == 0.0;
    }

    private boolean filterDistance(Position position, Position last) {
        if (filterDistance != 0 && last != null) {
            return position.getDouble(Position.KEY_DISTANCE) < filterDistance;
        }
        return false;
    }

    private boolean filterMaxSpeed(Position position, Position last) {
        if (filterMaxSpeed != 0 && last != null) {
            double distance = position.getDouble(Position.KEY_DISTANCE);
            double time = position.getFixTime().getTime() - last.getFixTime().getTime();
            return time > 0 && UnitsConverter.knotsFromMps(distance / (time / 1000)) > filterMaxSpeed;
        }
        return false;
    }

    private boolean filterMinPeriod(Position position, Position last) {
        if (filterMinPeriod != 0 && last != null) {
            long time = position.getFixTime().getTime() - last.getFixTime().getTime();
            return time > 0 && time < filterMinPeriod;
        }
        return false;
    }

    private boolean filterDailyLimit(Position position, Position last) {
        if (filterDailyLimit != 0
                && statisticsManager.messageStoredCount(position.getDeviceId()) >= filterDailyLimit) {
            long lastTime = last != null ? last.getFixTime().getTime() : 0;
            long interval = position.getFixTime().getTime() - lastTime;
            return filterDailyLimitInterval <= 0 || interval < filterDailyLimitInterval;
        }
        return false;
    }

    private boolean skipLimit(Position position, Position last) {
        if (skipLimit != 0 && last != null) {
            return (position.getServerTime().getTime() - last.getServerTime().getTime()) > skipLimit;
        }
        return false;
    }

    private boolean skipAttributes(Position position) {
        if (skipAttributes) {
            String string = AttributeUtil.lookup(cacheManager, Keys.FILTER_SKIP_ATTRIBUTES, position.getDeviceId());
            for (String attribute : string.split("[ ,]")) {
                if (position.hasAttribute(attribute)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean filter(Position position) {

        List<String> filterTypes = new LinkedList<>();

        // filter out invalid data
        if (filterInvalid(position)) {
            filterTypes.add("Invalid");
        }
        if (filterZero(position)) {
            filterTypes.add("Zero");
        }
        if (filterOutdated(position)) {
            filterTypes.add("Outdated");
        }
        if (filterFuture(position)) {
            filterTypes.add("Future");
        }
        if (filterPast(position)) {
            filterTypes.add("Past");
        }
        if (filterAccuracy(position)) {
            filterTypes.add("Accuracy");
        }
        if (filterApproximate(position)) {
            filterTypes.add("Approximate");
        }

        // filter out excessive data
        long deviceId = position.getDeviceId();
        Position last = cacheManager.getPosition(deviceId);
        if (filterDuplicate(position, last) && !skipLimit(position, last) && !skipAttributes(position)) {
            filterTypes.add("Duplicate");
        }
        if (filterStatic(position) && !skipLimit(position, last) && !skipAttributes(position)) {
            filterTypes.add("Static");
        }
        if (filterDistance(position, last) && !skipLimit(position, last) && !skipAttributes(position)) {
            filterTypes.add("Distance");
        }
        if (filterMaxSpeed(position, last)) {
            filterTypes.add("MaxSpeed");
        }
        if (filterMinPeriod(position, last)) {
            filterTypes.add("MinPeriod");
        }
        if (filterDailyLimit(position, last)) {
            filterTypes.add("DailyLimit");
        }

        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device.getCalendarId() > 0) {
            Calendar calendar = cacheManager.getObject(Calendar.class, device.getCalendarId());
            if (!calendar.checkMoment(position.getFixTime())) {
                filterTypes.add("Calendar");
            }
        }

        if (!filterTypes.isEmpty()) {
            LOGGER.info("Position filtered by {} filters from device: {}",
                    String.join(" ", filterTypes), device.getUniqueId());
            return true;
        }

        return false;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        callback.processed(filter(position));
    }

}
