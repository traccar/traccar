/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.config.Keys;

import java.util.TimerTask;

public class HealthCheckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckService.class);

    private SystemD systemD;

    private boolean enabled;
    private long period;

    public HealthCheckService() {
        if (!Context.getConfig().getBoolean(Keys.WEB_DISABLE_HEALTH_CHECK)
                && System.getProperty("os.name").toLowerCase().startsWith("linux")) {
            try {
                systemD = Native.load("systemd", SystemD.class);
                String watchdogTimer = System.getenv("WATCHDOG_USEC");
                if (watchdogTimer != null && !watchdogTimer.isEmpty()) {
                    period = Long.parseLong(watchdogTimer) / 1000 * 4 / 5;
                }
                if (period > 0) {
                    LOGGER.info("Health check enabled with period {}", period);
                    enabled = true;
                }
            } catch (UnsatisfiedLinkError e) {
                LOGGER.warn("No systemd support", e);
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getPeriod() {
        return period;
    }

    private String getUrl() {
        String address = Context.getConfig().getString(Keys.WEB_ADDRESS, "localhost");
        int port = Context.getConfig().getInteger(Keys.WEB_PORT);
        return "http://" + address + ":" + port + "/api/server";
    }

    public TimerTask createTask() {
        return new TimerTask() {
            @Override
            public void run() {
                LOGGER.debug("Health check running");
                int status = Context.getClient().target(getUrl()).request().get().getStatus();
                if (status == 200) {
                    int result = systemD.sd_notify(0, "WATCHDOG=1");
                    if (result < 0) {
                        LOGGER.warn("Health check notify error {}", result);
                    }
                } else {
                    LOGGER.warn("Health check failed with status {}", status);
                }
            }
        };
    }

    interface SystemD extends Library {
        @SuppressWarnings("checkstyle:MethodName")
        int sd_notify(@SuppressWarnings("checkstyle:ParameterName") int unset_environment, String state);
    }

}
