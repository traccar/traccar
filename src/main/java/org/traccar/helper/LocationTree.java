/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LocationTree {

    public static class Item {

        private Item left, right;
        private float x, y;
        private String data;

        public Item(float x, float y) {
            this(x, y, null);
        }

        public Item(float x, float y, String data) {
            this.x = x;
            this.y = y;
            this.data = data;
        }

        public String getData() {
            return data;
        }

        private float squaredDistance(Item item) {
            return (x - item.x) * (x - item.x) + (y - item.y) * (y - item.y);
        }

        private float axisSquaredDistance(Item item, int axis) {
            if (axis == 0) {
                return (x - item.x) * (x - item.x);
            } else {
                return (y - item.y) * (y - item.y);
            }
        }

    }

    private Item root;

    private ArrayList<Comparator<Item>> comparators = new ArrayList<>();

    public LocationTree(List<Item> items) {
        comparators.add(new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return Float.compare(o1.x, o2.x);
            }
        });
        comparators.add(new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return Float.compare(o1.y, o2.y);
            }
        });
        root = createTree(items, 0);
    }

    private Item createTree(List<Item> items, int depth) {
        if (items.isEmpty()) {
            return null;
        }
        Collections.sort(items, comparators.get(depth % 2));
        int currentIndex = items.size() / 2;
        Item median = items.get(currentIndex);
        median.left = createTree(new ArrayList<>(items.subList(0, currentIndex)), depth + 1);
        median.right = createTree(new ArrayList<>(items.subList(currentIndex + 1, items.size())), depth + 1);
        return median;
    }

    public Item findNearest(Item search) {
        return findNearest(root, search, 0);
    }

    private Item findNearest(Item current, Item search, int depth) {
        int direction = comparators.get(depth % 2).compare(search, current);

        Item next, other;
        if (direction < 0) {
            next = current.left;
            other = current.right;
        } else {
            next = current.right;
            other = current.left;
        }

        Item best = current;
        if (next != null) {
            best = findNearest(next, search, depth + 1);
        }

        if (current.squaredDistance(search) < best.squaredDistance(search)) {
            best = current;
        }
        if (other != null && current.axisSquaredDistance(search, depth % 2) < best.squaredDistance(search)) {
            Item possibleBest = findNearest(other, search, depth + 1);
            if (possibleBest.squaredDistance(search) < best.squaredDistance(search)) {
                best = possibleBest;
            }
        }

        return best;
    }

}
