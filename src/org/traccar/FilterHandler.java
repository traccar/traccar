/*
 * Copyright 2014 - 2016 Anton Tananaev (anton@traccar.org)
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

import org.traccar.helper.DistanceCalculator;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class FilterHandler extends BaseDataHandler {

    private boolean filterInvalid;
    private boolean filterZero;
    private boolean filterDuplicate;
    private boolean filterApproximate;
    private boolean filterStatic;
    private int filterDistance;
    private long filterLimit;
    private long filterFuture;

    public void setFilterInvalid(boolean filterInvalid) {
        this.filterInvalid = filterInvalid;
    }

    public void setFilterZero(boolean filterZero) {
        this.filterZero = filterZero;
    }

    public void setFilterDuplicate(boolean filterDuplicate) {
        this.filterDuplicate = filterDuplicate;
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

    public void setFilterLimit(long filterLimit) {
        this.filterLimit = filterLimit;
    }

    public void setFilterFuture(long filterFuture) {
        this.filterFuture = filterFuture;
    }

    public FilterHandler() {
        Config config = Context.getConfig();
        if (config != null) {
            filterInvalid = config.getBoolean("filter.invalid");
            filterZero = config.getBoolean("filter.zero");
            filterDuplicate = config.getBoolean("filter.duplicate");
            filterApproximate = config.getBoolean("filter.approximate");
            filterStatic = config.getBoolean("filter.static");
            filterDistance = config.getInteger("filter.distance");
            filterLimit = config.getLong("filter.limit") * 1000;
            filterFuture = config.getLong("filter.future") * 1000;
        }
    }

    private Position getLastPosition(long deviceId) {
        if (Context.getIdentityManager() != null) {
            return Context.getIdentityManager().getLastPosition(deviceId);
        }
        return null;
    }

    private boolean filterInvalid(Position position) {
        return filterInvalid && !position.getValid();
    }

    private boolean filterZero(Position position) {
        return filterZero && position.getLatitude() == 0.0 && position.getLongitude() == 0.0;
    }

    private boolean filterDuplicate(Position position) {
        if (filterDuplicate) {
            Position last = getLastPosition(position.getDeviceId());
            if (last != null) {
                return position.getFixTime().equals(last.getFixTime());
            } else {
                return false;
            }
        } else {
            return false;
        }
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

    private boolean filterDistance(Position position) {
        if (filterDistance != 0) {
            Position last = getLastPosition(position.getDeviceId());
            if (last != null) {
                double distance = DistanceCalculator.distance(
                        position.getLatitude(), position.getLongitude(),
                        last.getLatitude(), last.getLongitude());
                return distance < filterDistance;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean filterLimit(Position position) {
        if (filterLimit != 0) {
            Position last = getLastPosition(position.getDeviceId());
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

        if (filterInvalid(position)) {
            filterType.append("Invalid ");
        }
        if (filterZero(position)) {
            filterType.append("Zero ");
        }
        if (filterDuplicate(position)) {
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
        if (filterDistance(position)) {
            filterType.append("Distance ");
        }

        if (filterType.length() > 0 && !filterLimit(position)) {

            StringBuilder message = new StringBuilder();
            message.append("Position filtered by ");
            message.append(filterType.toString());
            message.append("filters from device: ");
            message.append(Context.getIdentityManager().getDeviceById(position.getDeviceId()).getUniqueId());
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
