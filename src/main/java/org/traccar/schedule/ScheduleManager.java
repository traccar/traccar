package org.traccar.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ScheduleManager {

    private ScheduledExecutorService executor;

    public void start() {

        executor = Executors.newSingleThreadScheduledExecutor();

        new TaskDeviceInactivityCheck().schedule(executor);

    }

    public void stop() {

        if (executor != null) {
            executor.shutdown();
            executor = null;
        }

    }

}
