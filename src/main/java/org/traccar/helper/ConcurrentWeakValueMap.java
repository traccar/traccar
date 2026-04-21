/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.helper;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConcurrentWeakValueMap<K, V> {

    private static final class WeakValue<K, V> extends WeakReference<V> {
        private final K key;
        WeakValue(K key, V value, ReferenceQueue<? super V> q) {
            super(value, q);
            this.key = key;
        }
    }

    private final Map<K, WeakValue<K, V>> map = new ConcurrentHashMap<>();
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();

    public V get(K key) {
        expunge();
        WeakValue<K, V> reference = map.get(key);
        return reference == null ? null : reference.get();
    }

    public void put(K key, V value) {
        expunge();
        map.put(key, new WeakValue<>(key, value, queue));
    }

    public V remove(K key) {
        expunge();
        WeakValue<K, V> reference = map.remove(key);
        return reference == null ? null : reference.get();
    }

    @SuppressWarnings("unchecked")
    private void expunge() {
        for (WeakValue<K, V> reference; (reference = (WeakValue<K, V>) queue.poll()) != null;) {
            map.remove(reference.key, reference);
        }
    }
}
