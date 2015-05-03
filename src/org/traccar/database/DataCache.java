/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class DataCache {
    
    private final Map<Long, Position> positions = new HashMap<Long, Position>();
    private final Map<Long, Set<DataCacheListener>> listeners = new HashMap<Long, Set<DataCacheListener>>();
    
    public DataCache(DataManager dataManager) {
        // TODO: load latest data from datavase
    }
    
    public synchronized void update(Position position) {
        long device = position.getDeviceId();
        positions.put(device, position);
        if (listeners.containsKey(device)) {
            for (DataCacheListener listener : listeners.get(device)) {
                listener.onUpdate(position);
            }
        }
    }
    
    public synchronized Collection<Position> getInitialState(Collection<Long> devices) {
        
        List<Position> result = new LinkedList<Position>();
        
        for (long device : devices) {
            if (positions.containsKey(device)) {
                result.add(positions.get(device));
            }
        }
        
        return result;
    }
    
    public static interface DataCacheListener {
        public void onUpdate(Position position);
    }
    
    public void addListener(Collection<Long> devices, DataCacheListener listener) {
        for (long device : devices) {
            addListener(device, listener);
        }
    }
    
    public synchronized void addListener(long device, DataCacheListener listener) {
        if (!listeners.containsKey(device)) {
            listeners.put(device, new HashSet<DataCacheListener>());
        }
        listeners.get(device).add(listener);
    }
    
    public void removeListener(Collection<Long> devices, DataCacheListener listener) {
        for (long device : devices) {
            removeListener(device, listener);
        }
    }
    
    public synchronized void removeListener(long device, DataCacheListener listener) {
        if (!listeners.containsKey(device)) {
            listeners.put(device, new HashSet<DataCacheListener>());
        }
        listeners.get(device).remove(listener);
    }
    
}
