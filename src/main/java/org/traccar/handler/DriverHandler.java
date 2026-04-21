/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler;

import jakarta.inject.Inject;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Driver;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

public class DriverHandler extends BasePositionHandler {

    private final CacheManager cacheManager;
    private final boolean useLinkedDriver;

    @Inject
    public DriverHandler(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        useLinkedDriver = config.getBoolean(Keys.PROCESSING_USE_LINKED_DRIVER);
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        if (useLinkedDriver && !position.hasAttribute(Position.KEY_DRIVER_UNIQUE_ID)) {
            var drivers = cacheManager.getDeviceObjects(position.getDeviceId(), Driver.class);
            if (!drivers.isEmpty()) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, drivers.iterator().next().getUniqueId());
            }
        }
        callback.processed(false);
    }

}
