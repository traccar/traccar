/*
 * Copyright 2014 - 2017 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import org.traccar.helper.Log;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

public class FilterHandler extends BaseDataHandler {

    private boolean filterInvalid;
    private boolean filterZero;
    private boolean filterDuplicate;
    private long filterFuture;
    private boolean filterApproximate;
    private boolean filterStatic;
    private int filterDistance;
    private int filterMaxSpeed;
    private long filterLimit;

    public void setFilterInvalid(boolean filterInvalid) {
        this.filterInvalid = filterInvalid;
    }

    public void setFilterZero(boolean filterZero) {
        this.filterZero = filterZero;
    }

    public void setFilterDuplicate(boolean filterDuplicate) {
        this.filterDuplicate = filterDuplicate;
    }

    public void setFilterFuture(long filterFuture) {
        this.filterFuture = filterFuture;
    }

    public void setFilterApproximate(boolean filterApproximate) {
        this.filterApproximate = filterApproximate;
    }

    public void setFilterStatic(boolean filterStatic) {
        this.filterStatic = filterStatic;
    }

    public void setFilterDistance(int filterDistance) {
        this.filterDistance = filterDistance;
    }

    public void setFilterMaxSpeed(int filterMaxSpeed) {
        this.filterMaxSpeed = filterMaxSpeed;
    }

    public void setFilterLimit(long filterLimit) {
        this.filterLimit = filterLimit;
    }

    public FilterHandler() {
        Config config = Context.getConfig();
        if (config != null) {
            filterInvalid = config.getBoolean("filter.invalid");
            filterZero = config.getBoolean("filter.zero");
            filterDuplicate = config.getBoolean("filter.duplicate");
            filterFuture = config.getLong("filter.future") * 1000;
            filterApproximate = config.getBoolean("filter.approximate");
            filterStatic = config.getBoolean("filter.static");
            filterDistance = config.getInteger("filter.distance");
            filterMaxSpeed = config.getInteger("filter.maxSpeed");
            filterLimit = config.getLong("filter.limit") * 1000;
        }
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
                if (!last.getAttributes().containsKey(key)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean filterFuture(Position position) {
        return filterFuture != 0 && position.getFixTime().getTime() > System.currentTimeMillis() + filterFuture;
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
            long time = position.getFixTime().getTime() - last.getFixTime().getTime();
            return UnitsConverter.knotsFromMps(distance / (time / 1000)) > filterMaxSpeed;
        }
        return false;
    }

    private boolean filterLimit(Position position, Position last) {
        if (filterLimit != 0) {
            if (last != null) {
                return (position.getFixTime().getTime() - last.getFixTime().getTime()) > filterLimit;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean filter(Position position) {

        StringBuilder filterType = new StringBuilder();

        Position last = null;
        if (Context.getIdentityManager() != null) {
            last = Context.getIdentityManager().getLastPosition(position.getDeviceId());
        }

        if (filterInvalid(position)) {
            filterType.append("Invalid ");
        }
        if (filterZero(position)) {
            filterType.append("Zero ");
        }
        if (filterDuplicate(position, last)) {
            filterType.append("Duplicate ");
        }
        if (filterFuture(position)) {
            filterType.append("Future ");
        }
        if (filterApproximate(position)) {
            filterType.append("Approximate ");
        }
        if (filterStatic(position)) {
            filterType.append("Static ");
        }
        if (filterDistance(position, last)) {
            filterType.append("Distance ");
        }
        if (filterMaxSpeed(position, last)) {
            filterType.append("MaxSpeed ");
        }

        if (filterType.length() > 0 && !filterLimit(position, last)) {

            StringBuilder message = new StringBuilder();
            message.append("Position filtered by ");
            message.append(filterType.toString());
            message.append("filters from device: ");
            message.append(Context.getIdentityManager().getById(position.getDeviceId()).getUniqueId());
            message.append(" with id: ");
            message.append(position.getDeviceId());

            Log.info(message.toString());
            return true;
        }

        return false;
    }

    @Override
    protected Position handlePosition(Position position) {
        if (filter(position)) {
            return null;
        }
        return position;
    }

}
