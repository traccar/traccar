/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.session.ConnectionManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskSessionTimeout implements ScheduleTask {

    private final ConnectionManager connectionManager;
    private final long period;

    @Inject
    public TaskSessionTimeout(Config config, ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        period = Math.max(config.getLong(Keys.STATUS_TIMEOUT) / 10, 1);
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, period, period, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        connectionManager.sweepIdleSessions();
    }

}
