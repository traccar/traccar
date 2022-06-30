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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Device;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeviceLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceLookupService.class);

    private final Storage storage;

    @Inject
    public DeviceLookupService(Storage storage) {
        this.storage = storage;
    }

    public Device lookup(String[] uniqueIds) {
        Device device = null;
        try {
            for (String uniqueId : uniqueIds) {
                device = storage.getObject(Device.class, new Request(
                        new Columns.All(), new Condition.Equals("uniqueId", "uniqueId", uniqueId)));
                if (device != null) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Find device error", e);
        }
        return device;
    }

}
