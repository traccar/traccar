/*
 * Copyright 2023 - 2026 Anton Tananaev (anton@traccar.org)
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CacheNode {

    private volatile BaseModel value;

    private final Map<Class<? extends BaseModel>, Set<CacheNode>> links = new ConcurrentHashMap<>();
    private final Map<Class<? extends BaseModel>, Set<CacheNode>> backlinks = new ConcurrentHashMap<>();

    public CacheNode(BaseModel value) {
        this.value = value;
    }

    public BaseModel getValue() {
        return value;
    }

    public void setValue(BaseModel value) {
        this.value = value;
    }

    public Stream<CacheNode> linkStream(Class<? extends BaseModel> clazz, boolean forward) {
        Set<CacheNode> set = (forward ? links : backlinks).get(clazz);
        return set != null ? set.stream() : Stream.empty();
    }

    public Stream<CacheNode> getAllLinks(boolean forward) {
        var map = forward ? links : backlinks;
        return map.values().stream().flatMap(Set::stream);
    }

    public void addLink(Class<? extends BaseModel> clazz, boolean forward, CacheNode node) {
        var map = forward ? links : backlinks;
        map.computeIfAbsent(clazz, key -> ConcurrentHashMap.newKeySet()).add(node);
    }

    public void removeLink(Class<? extends BaseModel> clazz, boolean forward, CacheNode node) {
        Set<CacheNode> set = (forward ? links : backlinks).get(clazz);
        if (set != null) {
            set.remove(node);
        }
    }

}
