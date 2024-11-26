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
package org.traccar.session.cache;

import org.traccar.model.BaseModel;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Stream;

public interface CacheGraph extends Serializable {
    void addObject(BaseModel value);

    void removeObject(Class<? extends BaseModel> clazz, long id);

    <T extends BaseModel> T getObject(Class<T> clazz, long id);

    <T extends BaseModel> Stream<T> getObjects(
            Class<? extends BaseModel> fromClass, long fromId,
            Class<T> clazz, Set<Class<? extends BaseModel>> proxies, boolean forward);

    void updateObject(BaseModel value);

    boolean addLink(
            Class<? extends BaseModel> fromClazz, long fromId,
            BaseModel toValue);

    void removeLink(Class<? extends BaseModel> fromClass, long fromId, Class<? extends BaseModel> toClass, long toId);
}
