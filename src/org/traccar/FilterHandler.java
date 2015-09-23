/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

    private final boolean filterInvalid;
    private final boolean filterZero;
    private final boolean filterDuplicate;
    private final int filterDistance;
    private final long filterLimit;
    
    public FilterHandler(
            boolean filterInvalid,
            boolean filterZero,
            boolean filterDuplicate,
            int filterDistance,
            long filterLimit) {

        this.filterInvalid = filterInvalid;
        this.filterZero = filterZero;
        this.filterDuplicate = filterDuplicate;
        this.filterDistance = filterDistance;
        this.filterLimit = filterLimit;
    }
    
    public FilterHandler() {
        Config config = Context.getConfig();

        filterInvalid = config.getBoolean("filter.invalid");
        filterZero = config.getBoolean("filter.zero");
        filterDuplicate = config.getBoolean("filter.duplicate");
        filterDistance = config.getInteger("filter.distance");
        filterLimit = config.getLong("filter.limit") * 1000;
    }
    
    private Position getLastPosition(long deviceId) {
        if (Context.getConnectionManager() != null) {
            return Context.getConnectionManager().getLastPosition(deviceId);
        }
        return null;
    }
    
    private boolean filterInvalid(Position position) {
        return filterInvalid && !position.getValid();
    }
    
    private boolean filterZero(Position position) {
        return filterZero &&
                (position.getLatitude() == 0.0) &&
                (position.getLongitude() == 0.0);
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
    
    private boolean filter(Position p) {
        
        boolean result =
                filterInvalid(p) ||
                filterZero(p) ||
                filterDuplicate(p) ||
                filterDistance(p);
        
        if (filterLimit(p)) {
            result = false;
        }
        
        if (result) {
            Log.info("Position filtered from " + p.getDeviceId());
        }

        return result;
    }

    @Override
    protected Position handlePosition(Position position) {
        return filter(position) ? null : position;
    }

}
