/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.traccar.Context;
import org.traccar.GlobalTimer;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class MotionManager {

    private final long stopTimeout;
    private final long motionTimeout;

    private final Map<Long, Timeout> stopTimeouts = new ConcurrentHashMap<>();
    private final Map<Long, Timeout> motionTimeouts = new ConcurrentHashMap<>();

    public MotionManager(long stopDelay, long motionDelay) {
        stopTimeout = stopDelay * 1000;
        motionTimeout = motionDelay * 1000;
    }

    public void updateDeviceMotion(final Position position, boolean oldMotion) {
        boolean motion = position.getBoolean(Position.KEY_MOTION);
        long deviceId = position.getDeviceId();
        if (!motion) {
            Timeout motionTimeout = motionTimeouts.remove(deviceId);
            if (motionTimeout != null) {
                motionTimeout.cancel();
            }
            if (oldMotion && (motionTimeout == null || !motionTimeout.isCancelled())
                    && !stopTimeouts.containsKey(deviceId)) {
                stopTimeouts.put(deviceId, GlobalTimer.getTimer().newTimeout(new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        if (!timeout.isCancelled() && Context.getNotificationManager() != null) {
                            Context.getNotificationManager().updateEvent(
                                    new Event(Event.TYPE_DEVICE_STOPPED, position.getDeviceId(), position.getId()),
                                    position);
                        }
                    }
                }, stopTimeout, TimeUnit.MILLISECONDS));
            }
        } else {
            Timeout stopTimeout = stopTimeouts.remove(deviceId);
            if (stopTimeout != null) {
                stopTimeout.cancel();
            }
            if (!oldMotion && (stopTimeout == null || !stopTimeout.isCancelled())
                    && !motionTimeouts.containsKey(deviceId)) {
                motionTimeouts.put(deviceId, GlobalTimer.getTimer().newTimeout(new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        if (!timeout.isCancelled() && Context.getNotificationManager() != null) {
                            Context.getNotificationManager().updateEvent(
                                    new Event(Event.TYPE_DEVICE_MOVING, position.getDeviceId(), position.getId()),
                                    position);
                        }
                    }
                }, motionTimeout, TimeUnit.MILLISECONDS));
            }
        }
    }

}
