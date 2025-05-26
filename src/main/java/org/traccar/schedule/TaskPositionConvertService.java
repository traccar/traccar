/*
 * Copyright 2025 Haven Madray (sgpublic2002@gmail.com)
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
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.LifecycleObject;
import org.traccar.geoconv.PositionConverter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaskPositionConvertService implements LifecycleObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPositionConvertService.class);

    private final Injector injector;

    private ScheduledExecutorService executor;

    @Inject
    public TaskPositionConvertService(Injector inject) {
        this.injector = inject;
    }

    @Override
    public void start() throws Exception {
        PositionConverter.ConverterInfo[] converters = PositionConverter.ConverterInfo.values();
        executor = Executors.newScheduledThreadPool(converters.length, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);  // 守护线程
            return t;
        });

        for (PositionConverter.ConverterInfo info : converters) {
            PositionConverter converter = injector.getInstance(info.getClazz());
            if (converter.enable()) {
                converter.schedule(executor);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}
