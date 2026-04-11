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

    private final CacheManager cacheManager;
    private final StatisticsManager statisticsManager;

    @Inject
    public FilterHandler(CacheManager cacheManager, StatisticsManager statisticsManager) {
        this.cacheManager = cacheManager;
        this.statisticsManager = statisticsManager;
    }

    private boolean filterInvalid(Position position) {
        Boolean filterInvalid = AttributeUtil.lookup(cacheManager, Keys.FILTER_INVALID, position.getDeviceId());
        return Boolean.TRUE.equals(filterInvalid) && (!position.getValid()
                || position.getLatitude() > 90 || position.getLongitude() > 180
                || position.getLatitude() < -90 || position.getLongitude() < -180);
    }

    private boolean filterZero(Position position) {
        Boolean filterZero = AttributeUtil.lookup(cacheManager, Keys.FILTER_ZERO, position.getDeviceId());
        return Boolean.TRUE.equals(filterZero) && position.getLatitude() == 0.0 && position.getLongitude() == 0.0;
    }

    private boolean filterDuplicate(Position position, Position last) {
        Boolean filterDuplicate = AttributeUtil.lookup(cacheManager, Keys.FILTER_DUPLICATE, position.getDeviceId());
        if (Boolean.TRUE.equals(filterDuplicate) && last != null && position.getFixTime().equals(last.getFixTime())) {
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
        Boolean filterOutdated = AttributeUtil.lookup(cacheManager, Keys.FILTER_OUTDATED, position.getDeviceId());
        return Boolean.TRUE.equals(filterOutdated) && position.getOutdated();
    }

    private boolean filterFuture(Position position) {
        Long filterFuture = AttributeUtil.lookup(cacheManager, Keys.FILTER_FUTURE, position.getDeviceId());
        return filterFuture != null
                && position.getFixTime().getTime() > System.currentTimeMillis() + filterFuture * 1000;
    }

    private boolean filterPast(Position position) {
        Long filterPast = AttributeUtil.lookup(cacheManager, Keys.FILTER_PAST, position.getDeviceId());
        return filterPast != null && position.getFixTime().getTime() < System.currentTimeMillis() - filterPast * 1000;
    }

    private boolean filterAccuracy(Position position) {
        Integer filterAccuracy = AttributeUtil.lookup(cacheManager, Keys.FILTER_ACCURACY, position.getDeviceId());
        return filterAccuracy != null && position.getAccuracy() > filterAccuracy;
    }

    private boolean filterApproximate(Position position) {
        Boolean filterApproximate = AttributeUtil.lookup(cacheManager, Keys.FILTER_APPROXIMATE, position.getDeviceId());
        return Boolean.TRUE.equals(filterApproximate) && position.getBoolean(Position.KEY_APPROXIMATE);
    }

    private boolean filterStatic(Position position) {
        Boolean filterStatic = AttributeUtil.lookup(cacheManager, Keys.FILTER_STATIC, position.getDeviceId());
        return Boolean.TRUE.equals(filterStatic) && position.getSpeed() == 0.0;
    }

    private boolean filterDistance(Position position, Position last) {
        Integer filterDistance = AttributeUtil.lookup(cacheManager, Keys.FILTER_DISTANCE, position.getDeviceId());
        if (filterDistance != null && last != null) {
            return position.getDouble(Position.KEY_DISTANCE) < filterDistance;
        }
        return false;
    }

    private boolean filterMaxSpeed(Position position, Position last) {
        Integer filterMaxSpeed = AttributeUtil.lookup(cacheManager, Keys.FILTER_MAX_SPEED, position.getDeviceId());
        if (filterMaxSpeed != null && last != null) {
            double distance = position.getDouble(Position.KEY_DISTANCE);
            double time = position.getFixTime().getTime() - last.getFixTime().getTime();
            return time > 0 && UnitsConverter.knotsFromMps(distance / (time / 1000)) > filterMaxSpeed;
        }
        return false;
    }

    private boolean filterMinPeriod(Position position, Position last) {
        Integer filterMinPeriod = AttributeUtil.lookup(cacheManager, Keys.FILTER_MIN_PERIOD, position.getDeviceId());
        if (filterMinPeriod != null && last != null) {
            long time = position.getFixTime().getTime() - last.getFixTime().getTime();
            return time > 0 && time < filterMinPeriod * 1000L;
        }
        return false;
    }

    private boolean filterDailyLimit(Position position, Position last) {
        long deviceId = position.getDeviceId();
        Integer filterDailyLimit = AttributeUtil.lookup(cacheManager, Keys.FILTER_DAILY_LIMIT, deviceId);
        if (filterDailyLimit != null && statisticsManager.messageStoredCount(deviceId) >= filterDailyLimit) {
            Integer filterDailyLimitInterval = AttributeUtil.lookup(
                    cacheManager, Keys.FILTER_DAILY_LIMIT_INTERVAL, deviceId);
            long lastTime = last != null ? last.getFixTime().getTime() : 0;
            long interval = position.getFixTime().getTime() - lastTime;
            return filterDailyLimitInterval == null || interval < filterDailyLimitInterval * 1000L;
        }
        return false;
    }

    private boolean skipLimit(Position position, Position last) {
        Long skipLimit = AttributeUtil.lookup(cacheManager, Keys.FILTER_SKIP_LIMIT, position.getDeviceId());
        if (skipLimit != null && last != null) {
            return (position.getServerTime().getTime() - last.getServerTime().getTime()) > skipLimit * 1000;
        }
        return false;
    }

    private boolean skipAttributes(Position position) {
        Boolean skipAttributes = AttributeUtil.lookup(
                cacheManager, Keys.FILTER_SKIP_ATTRIBUTES_ENABLE, position.getDeviceId());
        if (Boolean.TRUE.equals(skipAttributes)) {
            String string = AttributeUtil.lookup(cacheManager, Keys.FILTER_SKIP_ATTRIBUTES, position.getDeviceId());
            if (string != null) {
                for (String attribute : string.split("[ ,]")) {
                    if (position.hasAttribute(attribute)) {
                        return true;
                    }
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
