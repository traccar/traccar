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
package org.traccar.session.cache;

import org.traccar.model.BaseModel;

import java.util.Objects;

class CacheKey {

    private final Class<? extends BaseModel> clazz;
    private final long id;

    CacheKey(BaseModel object) {
        this(object.getClass(), object.getId());
    }

    CacheKey(Class<? extends BaseModel> clazz, long id) {
        this.clazz = clazz;
        this.id = id;
    }

    public boolean classIs(Class<? extends BaseModel> clazz) {
        return clazz.equals(this.clazz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CacheKey cacheKey = (CacheKey) o;
        return id == cacheKey.id && Objects.equals(clazz, cacheKey.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, id);
    }

}
