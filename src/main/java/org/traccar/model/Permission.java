/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
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

import java.beans.Introspector;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.traccar.helper.ClassScanner;
import org.traccar.storage.QueryIgnore;

public class Permission {

    private static final Map<String, Class<? extends BaseModel>> CLASSES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        try {
            for (Class<?> clazz : ClassScanner.findSubclasses(BaseModel.class)) {
                CLASSES.put(clazz.getSimpleName(), (Class<? extends BaseModel>) clazz);
            }
        } catch (IOException | ReflectiveOperationException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private final LinkedHashMap<String, Long> data;

    private final Class<? extends BaseModel> ownerClass;
    private final long ownerId;
    private final Class<? extends BaseModel> propertyClass;
    private final long propertyId;

    public Permission(LinkedHashMap<String, Long> data) {
        this.data = data;
        var iterator = data.entrySet().iterator();
        var owner = iterator.next();
        ownerClass = getKeyClass(owner.getKey());
        ownerId = owner.getValue();
        var property = iterator.next();
        propertyClass = getKeyClass(property.getKey());
        propertyId = property.getValue();
    }

    public Permission(
            Class<? extends BaseModel> ownerClass, long ownerId,
            Class<? extends BaseModel> propertyClass, long propertyId) {
        this.ownerClass = ownerClass;
        this.ownerId = ownerId;
        this.propertyClass = propertyClass;
        this.propertyId = propertyId;
        data = new LinkedHashMap<>();
        data.put(getKey(ownerClass), ownerId);
        data.put(getKey(propertyClass), propertyId);
    }

    public static Class<? extends BaseModel> getKeyClass(String key) {
        return CLASSES.get(key.substring(0, key.length() - 2));
    }

    public static String getKey(Class<?> clazz) {
        return Introspector.decapitalize(clazz.getSimpleName()) + "Id";
    }

    public static String getStorageName(Class<?> ownerClass, Class<?> propertyClass) {
        String ownerName = ownerClass.getSimpleName();
        String propertyName = propertyClass.getSimpleName();
        String managedPrefix = "Managed";
        if (propertyName.startsWith(managedPrefix)) {
            propertyName = propertyName.substring(managedPrefix.length());
        }
        return "tc_" + Introspector.decapitalize(ownerName) + "_" + Introspector.decapitalize(propertyName);
    }

    @QueryIgnore
    @JsonIgnore
    public String getStorageName() {
        return getStorageName(ownerClass, propertyClass);
    }

    @QueryIgnore
    @JsonAnyGetter
    public Map<String, Long> get() {
        return data;
    }

    @QueryIgnore
    @JsonAnySetter
    public void set(String key, Long value) {
        data.put(key, value);
    }

    @QueryIgnore
    @JsonIgnore
    public Class<? extends BaseModel> getOwnerClass() {
        return ownerClass;
    }

    @QueryIgnore
    @JsonIgnore
    public long getOwnerId() {
        return ownerId;
    }

    @QueryIgnore
    @JsonIgnore
    public Class<? extends BaseModel> getPropertyClass() {
        return propertyClass;
    }

    @QueryIgnore
    @JsonIgnore
    public long getPropertyId() {
        return propertyId;
    }

}
