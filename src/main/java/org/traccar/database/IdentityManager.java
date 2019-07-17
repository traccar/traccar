/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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

import org.traccar.model.Device;
import org.traccar.model.Position;

public interface IdentityManager {

    long addUnknownDevice(String uniqueId);

    Device getById(long id);

    Device getByUniqueId(String uniqueId) throws Exception;

    String getDevicePassword(long id, String protocol, String defaultPassword);

    Position getLastPosition(long deviceId);

    boolean isLatestPosition(Position position);

    boolean lookupAttributeBoolean(
            long deviceId, String attributeName, boolean defaultValue, boolean lookupServer, boolean lookupConfig);

    String lookupAttributeString(
            long deviceId, String attributeName, String defaultValue, boolean lookupServer, boolean lookupConfig);

    int lookupAttributeInteger(
            long deviceId, String attributeName, int defaultValue, boolean lookupServer, boolean lookupConfig);

    long lookupAttributeLong(
            long deviceId, String attributeName, long defaultValue, boolean lookupServer, boolean lookupConfig);

    double lookupAttributeDouble(
            long deviceId, String attributeName, double defaultValue, boolean lookupServer, boolean lookupConfig);

}
