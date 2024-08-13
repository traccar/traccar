/*
 * Copyright 2020 - 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.schedule;

import com.google.inject.Injector;
import org.traccar.LifecycleObject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

@Singleton
public class ScheduleManager implements LifecycleObject {

    private final Injector injector;
    private final boolean secondary;
    private ScheduledExecutorService executor;

    @Inject
    public ScheduleManager(Injector injector, Config config) {
        this.injector = injector;
        secondary = config.getBoolean(Keys.BROADCAST_SECONDARY);
    }

    @Override
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor();
        Stream.of(
                TaskHealthCheck.class,
                TaskClearStatus.class,
                TaskExpirations.class,
                TaskDeleteTemporary.class,
                TaskReports.class,
                TaskDeviceInactivityCheck.class,
                TaskWebSocketKeepalive.class)
                .forEachOrdered(taskClass -> {
                    var task = injector.getInstance(taskClass);
                    if (task.multipleInstances() || !secondary) {
                        task.schedule(executor);
                    }
                });
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

}
