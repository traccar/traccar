/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.AttributeAlias;

public class AliasesManager {

    private final DataManager dataManager;

    private final Map<Long, Set<AttributeAlias>> deviceAliases = new ConcurrentHashMap<>();
    private final Map<Long, AttributeAlias> aliasesById = new ConcurrentHashMap<>();

    public AliasesManager(DataManager dataManager) {
        this.dataManager = dataManager;
        if (dataManager != null) {
            try {
                for (AttributeAlias attributeAlias : dataManager.getAttributeAliases()) {
                    getAttributeAliases(attributeAlias.getDeviceId())
                            .add(attributeAlias);
                    aliasesById.put(attributeAlias.getId(), attributeAlias);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public Set<AttributeAlias> getAttributeAliases(long deviceId) {
        if (!deviceAliases.containsKey(deviceId)) {
            deviceAliases.put(deviceId, new HashSet<AttributeAlias>());
        }
        return deviceAliases.get(deviceId);
    }

    public void removeDevice(long deviceId) {
        for (AttributeAlias attributeAlias : getAttributeAliases(deviceId)) {
            aliasesById.remove(attributeAlias.getId());
        }
        deviceAliases.remove(deviceId);
    }

    public void addAttributeAlias(AttributeAlias attributeAlias) throws SQLException {
        dataManager.addAttributeAlias(attributeAlias);
        aliasesById.put(attributeAlias.getId(), attributeAlias);
        getAttributeAliases(attributeAlias.getDeviceId()).add(attributeAlias);
    }

    public void updateAttributeAlias(AttributeAlias attributeAlias) throws SQLException {
        dataManager.updateAttributeAlias(attributeAlias);
        AttributeAlias cachedAlias = aliasesById.get(attributeAlias.getId());
        if (cachedAlias.getDeviceId() != attributeAlias.getDeviceId()) {
            getAttributeAliases(cachedAlias.getDeviceId()).remove(cachedAlias);
            cachedAlias.setDeviceId(attributeAlias.getDeviceId());
            getAttributeAliases(cachedAlias.getDeviceId()).add(cachedAlias);
        }
        cachedAlias.setAttribute(attributeAlias.getAttribute());
        cachedAlias.setAlias(attributeAlias.getAlias());
    }

    public void removeArrtibuteAlias(long attributeAliasId) throws SQLException {
        dataManager.removeAttributeAlias(attributeAliasId);
        AttributeAlias cachedAlias = aliasesById.get(attributeAliasId);
        getAttributeAliases(cachedAlias.getDeviceId()).remove(cachedAlias);
        aliasesById.remove(attributeAliasId);
    }

    public AttributeAlias getAttributeAlias(long deviceId, String attribute) {
        for (AttributeAlias alias : getAttributeAliases(deviceId)) {
            if (alias.getAttribute().equals(attribute)) {
                return alias;
            }
        }
        return null;
    }

    public Collection<AttributeAlias> getAllAttributeAliases(long userId) {
        Collection<AttributeAlias> userDevicesAliases = new ArrayList<>();
        for (long deviceId : Context.getPermissionsManager().getDevicePermissions(userId)) {
            userDevicesAliases.addAll(getAttributeAliases(deviceId));
        }
        return userDevicesAliases;
    }

    public AttributeAlias getAttributeAlias(long id) {
        return aliasesById.get(id);
    }

}
