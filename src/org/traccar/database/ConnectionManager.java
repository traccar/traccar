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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.traccar.Context;
import org.traccar.GlobalTimer;
import org.traccar.Protocol;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class ConnectionManager {

    private static final long DEFAULT_TIMEOUT = 600;

    private final long deviceTimeout;

    private final Map<Long, ActiveDevice> activeDevices = new HashMap<>();
    private final Map<Long, Position> positions = new HashMap<>();
    private final Map<Long, Set<UpdateListener>> listeners = new HashMap<>();
    private final Map<Long, Timeout> timeouts = new HashMap<>();

    public ConnectionManager(DataManager dataManager) {
        deviceTimeout = Context.getConfig().getLong("status.timeout", DEFAULT_TIMEOUT) * 1000;
        if (dataManager != null) {
            try {
                for (Position position : dataManager.getLatestPositions()) {
                    positions.put(position.getDeviceId(), position);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public void addActiveDevice(long deviceId, Protocol protocol, Channel channel, SocketAddress remoteAddress) {
        activeDevices.put(deviceId, new ActiveDevice(deviceId, protocol, channel, remoteAddress));
    }

    public void removeActiveDevice(Channel channel) {
        for (ActiveDevice activeDevice : activeDevices.values()) {
            if (activeDevice.getChannel() == channel) {
                updateDevice(activeDevice.getDeviceId(), Device.STATUS_OFFLINE, null);
                activeDevices.remove(activeDevice.getDeviceId());
                break;
            }
        }
    }

    public ActiveDevice getActiveDevice(long deviceId) {
        return activeDevices.get(deviceId);
    }

    public synchronized void updateDevice(final long deviceId, String status, Date time) {
        Device device = Context.getIdentityManager().getDeviceById(deviceId);
        device.setStatus(status);
        if (time != null) {
            device.setLastUpdate(time);
        }

        Timeout timeout = timeouts.remove(deviceId);
        if (timeout != null) {
            timeout.cancel();
        }

        if (status.equals(Device.STATUS_ONLINE)) {
            timeouts.put(deviceId, GlobalTimer.getTimer().newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    if (!timeout.isCancelled()) {
                        updateDevice(deviceId, Device.STATUS_UNKNOWN, null);
                    }
                }
            }, deviceTimeout, TimeUnit.MILLISECONDS));
        }

        try {
            Context.getDataManager().updateDeviceStatus(device);
        } catch (SQLException error) {
            Log.warning(error);
        }

        if (listeners.containsKey(deviceId)) {
            for (UpdateListener listener : listeners.get(deviceId)) {
                listener.onUpdateDevice(device);
            }
        }
    }

    public synchronized void updatePosition(Position position) {
        long deviceId = position.getDeviceId();
        positions.put(deviceId, position);

        if (listeners.containsKey(deviceId)) {
            for (UpdateListener listener : listeners.get(deviceId)) {
                listener.onUpdatePosition(position);
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

    public interface UpdateListener {
        void onUpdateDevice(Device device);
        void onUpdatePosition(Position position);
    }

    public void addListener(Collection<Long> devices, UpdateListener listener) {
        for (long deviceId : devices) {
            addListener(deviceId, listener);
        }
    }

    public synchronized void addListener(long deviceId, UpdateListener listener) {
        if (!listeners.containsKey(deviceId)) {
            listeners.put(deviceId, new HashSet<UpdateListener>());
        }
        listeners.get(deviceId).add(listener);
    }

    public void removeListener(Collection<Long> devices, UpdateListener listener) {
        for (long deviceId : devices) {
            removeListener(deviceId, listener);
        }
    }

    public synchronized void removeListener(long deviceId, UpdateListener listener) {
        if (!listeners.containsKey(deviceId)) {
            listeners.put(deviceId, new HashSet<UpdateListener>());
        }
        listeners.get(deviceId).remove(listener);
    }

}
