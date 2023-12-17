/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class WeakValueMap<K, V> {

    private final Map<K, WeakReference<V>> map = new HashMap<>();

    public void put(K key, V value) {
        map.put(key, new WeakReference<>(value));
    }

    public V get(K key) {
        WeakReference<V> weakReference = map.get(key);
        return (weakReference != null) ? weakReference.get() : null;
    }

    public V remove(K key) {
        WeakReference<V> weakReference = map.remove(key);
        return (weakReference != null) ? weakReference.get() : null;
    }

    private void clean() {
        map.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }

}
