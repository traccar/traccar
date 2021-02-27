/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.model;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.traccar.database.DataManager;

public class Permission {

    private final Class<?> ownerClass;
    private final long ownerId;
    private final Class<?> propertyClass;
    private final long propertyId;

    public Permission(LinkedHashMap<String, Long> permissionMap) throws ClassNotFoundException {
        Iterator<Map.Entry<String, Long>> iterator = permissionMap.entrySet().iterator();
        String owner = iterator.next().getKey();
        ownerClass = DataManager.getClassByName(owner);
        String property = iterator.next().getKey();
        propertyClass = DataManager.getClassByName(property);
        ownerId = permissionMap.get(owner);
        propertyId = permissionMap.get(property);
    }

    public Class<?> getOwnerClass() {
        return ownerClass;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public Class<?> getPropertyClass() {
        return propertyClass;
    }

    public long getPropertyId() {
        return propertyId;
    }
}
