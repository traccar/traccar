/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.traccar.Context;
import org.traccar.GlobalTimer;
import org.traccar.Protocol;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ConnectionManager {

    private static final long DEFAULT_TIMEOUT = 600;

    private final long deviceTimeout;
    private final boolean enableStatusEvents;

    private final Map<Long, ActiveDevice> activeDevices = new ConcurrentHashMap<>();
    private final Map<Long, Set<UpdateListener>> listeners = new ConcurrentHashMap<>();
    private final Map<Long, Timeout> timeouts = new ConcurrentHashMap<>();

    public ConnectionManager() {
        deviceTimeout = Context.getConfig().getLong("status.timeout", DEFAULT_TIMEOUT) * 1000;
        enableStatusEvents = Context.getConfig().getBoolean("event.statusHandler");
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

    public void updateDevice(final long deviceId, String status, Date time) {
        Device device = Context.getIdentityManager().getDeviceById(deviceId);
        if (device == null) {
            return;
        }

        String oldStatus = device.getStatus();
        device.setStatus(status);

        if (enableStatusEvents && !status.equals(oldStatus)) {
            String eventType;
            switch (status) {
                case Device.STATUS_ONLINE:
                    eventType = Event.TYPE_DEVICE_ONLINE;
                    break;
                case Device.STATUS_UNKNOWN:
                    eventType = Event.TYPE_DEVICE_UNKNOWN;
                    break;
                default:
                    eventType = Event.TYPE_DEVICE_OFFLINE;
                    break;
            }
            Event event = new Event(eventType, deviceId);
            if (Context.getNotificationManager() != null) {
                Context.getNotificationManager().updateEvent(event, null);
            }
        }

        Timeout timeout = timeouts.remove(deviceId);
        if (timeout != null) {
            timeout.cancel();
        }

        if (time != null) {
            device.setLastUpdate(time);
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
            Context.getDeviceManager().updateDeviceStatus(device);
        } catch (SQLException error) {
            Log.warning(error);
        }

        updateDevice(device);
    }

    public synchronized void updateDevice(Device device) {
        for (long userId : Context.getPermissionsManager().getDeviceUsers(device.getId())) {
            if (listeners.containsKey(userId)) {
                for (UpdateListener listener : listeners.get(userId)) {
                    listener.onUpdateDevice(device);
                }
            }
        }
    }

    public synchronized void updatePosition(Position position) {
        long deviceId = position.getDeviceId();

        for (long userId : Context.getPermissionsManager().getDeviceUsers(deviceId)) {
            if (listeners.containsKey(userId)) {
                for (UpdateListener listener : listeners.get(userId)) {
                    listener.onUpdatePosition(position);
                }
            }
        }
    }

    public synchronized void updateEvent(long userId, Event event) {
        if (listeners.containsKey(userId)) {
            for (UpdateListener listener : listeners.get(userId)) {
                listener.onUpdateEvent(event);
            }
        }
    }

    public interface UpdateListener {
        void onUpdateDevice(Device device);
        void onUpdatePosition(Position position);
        void onUpdateEvent(Event event);
    }

    public synchronized void addListener(long userId, UpdateListener listener) {
        if (!listeners.containsKey(userId)) {
            listeners.put(userId, new HashSet<UpdateListener>());
        }
        listeners.get(userId).add(listener);
    }

    public synchronized void removeListener(long userId, UpdateListener listener) {
        if (!listeners.containsKey(userId)) {
            listeners.put(userId, new HashSet<UpdateListener>());
        }
        listeners.get(userId).remove(listener);
    }

}
