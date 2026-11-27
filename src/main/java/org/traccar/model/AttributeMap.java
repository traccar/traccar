/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class AttributeMap extends AbstractMap<String, Object> {

    private static final Object[] EMPTY = new Object[0];
    private static final int INITIAL_CAPACITY = 8;

    private Object[] data = EMPTY;
    private int size;

    public AttributeMap() {}

    public AttributeMap(Map<? extends String, ?> source) {
        if (source != null) {
            putAll(source);
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return indexOf(key) >= 0;
    }

    @Override
    public Object get(Object key) {
        int index = indexOf(key);
        return index < 0 ? null : data[index + 1];
    }

    @Override
    public Object put(String key, Object value) {
        int index = indexOf(key);
        if (index >= 0) {
            Object previous = data[index + 1];
            data[index + 1] = value;
            return previous;
        }
        ensureCapacity();
        data[size * 2] = key;
        data[size * 2 + 1] = value;
        size++;
        return null;
    }

    @Override
    public Object remove(Object key) {
        int index = indexOf(key);
        if (index < 0) {
            return null;
        }
        Object previous = data[index + 1];
        int lastIndex = (size - 1) * 2;
        if (index < lastIndex) {
            System.arraycopy(data, index + 2, data, index, lastIndex - index);
        }
        data[lastIndex] = null;
        data[lastIndex + 1] = null;
        size--;
        return previous;
    }

    @Override
    public void clear() {
        int limit = size * 2;
        for (int index = 0; index < limit; index++) {
            data[index] = null;
        }
        size = 0;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new EntrySet();
    }

    private int indexOf(Object key) {
        Object[] localData = data;
        int limit = size * 2;
        for (int index = 0; index < limit; index += 2) {
            Object current = localData[index];
            if (key == current || key.equals(current)) {
                return index;
            }
        }
        return -1;
    }

    private void ensureCapacity() {
        int required = (size + 1) * 2;
        if (data.length < required) {
            int newLength = data.length == 0 ? INITIAL_CAPACITY : data.length * 2;
            data = Arrays.copyOf(data, newLength);
        }
    }

    private final class EntrySet extends AbstractSet<Entry<String, Object>> {
        @Override
        public Iterator<Entry<String, Object>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return size;
        }
    }

    private final class EntryIterator implements Iterator<Entry<String, Object>> {

        private int cursor;
        private int lastReturned = -1;

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public Entry<String, Object> next() {
            if (cursor >= size) {
                throw new NoSuchElementException();
            }
            lastReturned = cursor;
            int index = cursor * 2;
            cursor++;
            return new ArrayEntry(index);
        }

        @Override
        public void remove() {
            if (lastReturned < 0) {
                throw new IllegalStateException();
            }
            AttributeMap.this.remove(data[lastReturned * 2]);
            cursor = lastReturned;
            lastReturned = -1;
        }
    }

    private final class ArrayEntry implements Entry<String, Object> {

        private final int index;

        ArrayEntry(int index) {
            this.index = index;
        }

        @Override
        public String getKey() {
            return (String) data[index];
        }

        @Override
        public Object getValue() {
            return data[index + 1];
        }

        @Override
        public Object setValue(Object value) {
            Object previous = data[index + 1];
            data[index + 1] = value;
            return previous;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Entry<?, ?> entry)) {
                return false;
            }
            Object key = getKey();
            Object value = getValue();
            return (key == null ? entry.getKey() == null : key.equals(entry.getKey()))
                    && (value == null ? entry.getValue() == null : value.equals(entry.getValue()));
        }

        @Override
        public int hashCode() {
            Object value = getValue();
            return getKey().hashCode() ^ (value == null ? 0 : value.hashCode());
        }
    }

}
