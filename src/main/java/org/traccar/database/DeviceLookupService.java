/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class DeviceLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceLookupService.class);

    private static final long INFO_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(60);
    private static final long THROTTLE_MIN_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long THROTTLE_MAX_MS = TimeUnit.MINUTES.toMillis(30);

    private final Storage storage;
    private final Timer timer;

    private final boolean throttlingEnabled;

    private static final class IdentifierInfo {
        private long lastQuery;
        private long delay;
        private Timeout timeout;
    }

    private final class IdentifierTask implements TimerTask {
        private final String uniqueId;

        private IdentifierTask(String uniqueId) {
            this.uniqueId = uniqueId;
        }

        @Override
        public void run(Timeout timeout) {
            LOGGER.debug("Device lookup expired {}", uniqueId);
            synchronized (DeviceLookupService.this) {
                identifierMap.remove(uniqueId);
            }
        }
    }

    private final Map<String, IdentifierInfo> identifierMap = new ConcurrentHashMap<>();

    @Inject
    public DeviceLookupService(Config config, Storage storage, Timer timer) {
        this.storage = storage;
        this.timer = timer;
        throttlingEnabled = config.getBoolean(Keys.DATABASE_THROTTLE_UNKNOWN);
    }

    private synchronized boolean isThrottled(String uniqueId) {
        if (throttlingEnabled) {
            IdentifierInfo info = identifierMap.get(uniqueId);
            return info != null && System.currentTimeMillis() < info.lastQuery + info.delay;
        } else {
            return false;
        }
    }

    private synchronized void lookupSucceeded(String uniqueId) {
        if (throttlingEnabled) {
            IdentifierInfo info = identifierMap.remove(uniqueId);
            if (info != null) {
                info.timeout.cancel();
            }
        }
    }

    private synchronized void lookupFailed(String uniqueId) {
        if (throttlingEnabled) {
            IdentifierInfo info = identifierMap.get(uniqueId);
            if (info != null) {
                info.timeout.cancel();
                info.delay = Math.min(info.delay * 2, THROTTLE_MAX_MS);
            } else {
                info = new IdentifierInfo();
                identifierMap.put(uniqueId, info);
                info.delay = THROTTLE_MIN_MS;
            }
            info.lastQuery = System.currentTimeMillis();
            info.timeout = timer.newTimeout(new IdentifierTask(uniqueId), INFO_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            LOGGER.debug("Device lookup {} throttled for {} ms", uniqueId, info.delay);
        }
    }

    public Device lookup(String[] uniqueIds) {
        Device device = null;
        try {
            for (String uniqueId : uniqueIds) {
                if (!isThrottled(uniqueId)) {
                    device = storage.getObject(Device.class, new Request(
                            new Columns.All(), new Condition.Equals("uniqueId", uniqueId)));
                    if (device != null) {
                        lookupSucceeded(uniqueId);
                        break;
                    } else {
                        lookupFailed(uniqueId);
                    }
                } else {
                    LOGGER.debug("Device lookup throttled {}", uniqueId);
                }
            }
        } catch (StorageException e) {
            LOGGER.warn("Find device error", e);
        }
        return device;
    }

}
