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

import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.netty.channel.Channel;
import org.traccar.Protocol;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class ConnectionManager {

    private final Map<Long, ActiveDevice> activeDevices = new HashMap<>();
    private final Map<Long, Position> positions = new HashMap<>();
    private final Map<Long, Set<DataCacheListener>> listeners = new HashMap<>();

    public ConnectionManager(DataManager dataManager) {
        if (dataManager != null) {
            try {
                for (Position position : dataManager.getLatestPositions()) {
                    this.positions.put(position.getDeviceId(), position);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public void setActiveDevice(long deviceId, Protocol protocol, Channel channel, SocketAddress remoteAddress) {
        activeDevices.put(deviceId, new ActiveDevice(deviceId, protocol, channel, remoteAddress));
    }

    public void removeActiveDevice(Channel channel) {
        for (ActiveDevice activeDevice : activeDevices.values()) {
            if (activeDevice.getChannel() == channel) {
                activeDevices.remove(activeDevice.getDeviceId());
                break;
            }
        }
    }

    public ActiveDevice getActiveDevice(long deviceId) {
        return activeDevices.get(deviceId);
    }

    public synchronized void update(Position position) {
        long deviceId = position.getDeviceId();
        positions.put(deviceId, position);
        if (listeners.containsKey(deviceId)) {
            for (DataCacheListener listener : listeners.get(deviceId)) {
                listener.onUpdate(position);
            }
        }
    }

    public Position getLastPosition(long deviceId) {
        return positions.get(deviceId);
    }

    public synchronized Collection<Position> getInitialState(Collection<Long> devices) {

        List<Position> result = new LinkedList<>();

        for (long device : devices) {
            if (positions.containsKey(device)) {
                result.add(positions.get(device));
            }
        }

        return result;
    }

    public interface DataCacheListener {
        void onUpdate(Position position);
    }

    public void addListener(Collection<Long> devices, DataCacheListener listener) {
        for (long deviceId : devices) {
            addListener(deviceId, listener);
        }
    }

    public synchronized void addListener(long deviceId, DataCacheListener listener) {
        if (!listeners.containsKey(deviceId)) {
            listeners.put(deviceId, new HashSet<DataCacheListener>());
        }
        listeners.get(deviceId).add(listener);
    }

    public void removeListener(Collection<Long> devices, DataCacheListener listener) {
        for (long deviceId : devices) {
            removeListener(deviceId, listener);
        }
    }

    public synchronized void removeListener(long deviceId, DataCacheListener listener) {
        if (!listeners.containsKey(deviceId)) {
            listeners.put(deviceId, new HashSet<DataCacheListener>());
        }
        listeners.get(deviceId).remove(listener);
    }

}
