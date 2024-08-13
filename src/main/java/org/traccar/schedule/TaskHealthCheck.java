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

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import org.traccar.database.StatisticsManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskHealthCheck implements ScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskHealthCheck.class);

    private final Config config;
    private final Client client;
    private final StatisticsManager statisticsManager;

    private final long gracePeriod = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);

    private SystemD systemD;

    private boolean enabled;
    private long period;
    private double dropThreshold;

    private int messageLastTotal;
    private int messageLastPeriod;

    @Inject
    public TaskHealthCheck(Config config, Client client, StatisticsManager statisticsManager) {
        this.config = config;
        this.client = client;
        this.statisticsManager = statisticsManager;
        if (!config.getBoolean(Keys.WEB_DISABLE_HEALTH_CHECK)
                && System.getProperty("os.name").toLowerCase().startsWith("linux")) {
            try {
                systemD = Native.load("systemd", SystemD.class);
                String watchdogTimer = System.getenv("WATCHDOG_USEC");
                if (watchdogTimer != null && !watchdogTimer.isEmpty()) {
                    period = Long.parseLong(watchdogTimer) / 1000 * 4 / 5;
                }
                if (period > 0) {
                    LOGGER.info("Health check enabled with period {}", period);
                    dropThreshold = config.getDouble(Keys.WEB_HEALTH_CHECK_DROP_THRESHOLD);
                    enabled = true;
                }
            } catch (UnsatisfiedLinkError e) {
                LOGGER.warn("No systemd support", e);
            }
        }
    }

    private String getUrl() {
        String address = config.getString(Keys.WEB_ADDRESS, "localhost");
        int port = config.getInteger(Keys.WEB_PORT);
        return "http://" + address + ":" + port + "/api/server";
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        if (enabled) {
            executor.scheduleAtFixedRate(this, period, period, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void run() {
        LOGGER.debug("Health check running");
        if (System.currentTimeMillis() > gracePeriod) {
            boolean success = true;

            int status = client.target(getUrl()).request().get().getStatus();
            if (status != 200) {
                success = false;
                LOGGER.warn("Web health check failed with status {}", status);
            }

            int messageCurrentTotal = statisticsManager.messageStoredCount();
            int messageCurrentPeriod = messageCurrentTotal - messageLastTotal;
            if (dropThreshold > 0 && messageLastPeriod > 0) {
                double drop = messageCurrentPeriod / (double) messageLastPeriod;
                if (drop < dropThreshold) {
                    success = false;
                    LOGGER.warn("Message health check failed with drop {}", drop);
                }
            }
            messageLastTotal = messageCurrentTotal;
            messageLastPeriod = messageCurrentPeriod;

            if (success) {
                notifyWatchdog();
            }
        } else {
            notifyWatchdog();
        }
    }

    private void notifyWatchdog() {
        int result = systemD.sd_notify(0, "WATCHDOG=1");
        if (result < 0) {
            LOGGER.warn("Health check notify error {}", result);
        }
    }

    interface SystemD extends Library {
        @SuppressWarnings("checkstyle:MethodName")
        int sd_notify(@SuppressWarnings("checkstyle:ParameterName") int unset_environment, String state);
    }

}
