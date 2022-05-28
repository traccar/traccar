/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static Injector injector;

    public static Injector getInjector() {
        return injector;
    }

    private Main() {
    }

    public static void logSystemInfo() {
        try {
            OperatingSystemMXBean operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
            LOGGER.info("Operating system"
                    + " name: " + operatingSystemBean.getName()
                    + " version: " + operatingSystemBean.getVersion()
                    + " architecture: " + operatingSystemBean.getArch());

            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            LOGGER.info("Java runtime"
                    + " name: " + runtimeBean.getVmName()
                    + " vendor: " + runtimeBean.getVmVendor()
                    + " version: " + runtimeBean.getVmVersion());

            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            LOGGER.info("Memory limit"
                    + " heap: " + memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024) + "mb"
                    + " non-heap: " + memoryBean.getNonHeapMemoryUsage().getMax() / (1024 * 1024) + "mb");

            LOGGER.info("Character encoding: "
                    + System.getProperty("file.encoding") + " charset: " + Charset.defaultCharset());

        } catch (Exception error) {
            LOGGER.warn("Failed to get system info");
        }
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);

        final String configFile;
        if (args.length <= 0) {
            configFile = "./debug.xml";
            if (!new File(configFile).exists()) {
                throw new RuntimeException("Configuration file is not provided");
            }
        } else {
            configFile = args[args.length - 1];
        }

        if (args.length > 0 && args[0].startsWith("--")) {
            WindowsService windowsService = new WindowsService("traccar") {
                @Override
                public void run() {
                    Main.run(configFile);
                }
            };
            switch (args[0]) {
                case "--install":
                    windowsService.install("traccar", null, null, null, null, configFile);
                    return;
                case "--uninstall":
                    windowsService.uninstall();
                    return;
                case "--service":
                default:
                    windowsService.init();
                    break;
            }
        } else {
            run(configFile);
        }
    }

    public static void run(String configFile) {
        try {
            Context.init(configFile);
            injector = Guice.createInjector(new MainModule());
            logSystemInfo();
            LOGGER.info("Version: " + Main.class.getPackage().getImplementationVersion());
            LOGGER.info("Starting server...");

            List<LifecycleObject> services = new LinkedList<>();
            services.add(Context.getServerManager());
            services.add(Context.getWebServer());
            services.add(Context.getScheduleManager());
            services.add(Context.getBroadcastService());

            for (LifecycleObject service : services) {
                if (service != null) {
                    service.start();
                }
            }

            Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("Thread exception", e));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down server...");

                Collections.reverse(services);
                for (LifecycleObject service : services) {
                    if (service != null) {
                        service.stop();
                    }
                }
            }));
        } catch (Exception e) {
            LOGGER.error("Main method error", e);
            throw new RuntimeException(e);
        }
    }

}
