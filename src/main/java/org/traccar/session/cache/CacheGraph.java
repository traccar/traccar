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

import org.traccar.model.BaseModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class CacheGraph {

    private final Map<CacheKey, CacheNode> roots = new HashMap<>();
    private final WeakValueMap<CacheKey, CacheNode> nodes = new WeakValueMap<>();

    void addObject(BaseModel value) {
        CacheKey key = new CacheKey(value);
        CacheNode node = new CacheNode(value);
        roots.put(key, node);
        nodes.put(key, node);
    }

    void removeObject(Class<? extends BaseModel> clazz, long id) {
        CacheKey key = new CacheKey(clazz, id);
        CacheNode node = nodes.remove(key);
        if (node != null) {
            node.getAllLinks(false).forEach(child -> child.getLinks(key.clazz(), true).remove(node));
        }
        roots.remove(key);
    }

    @SuppressWarnings("unchecked")
    <T extends BaseModel> T getObject(Class<T> clazz, long id) {
        CacheNode node = nodes.get(new CacheKey(clazz, id));
        return node != null ? (T) node.getValue() : null;
    }

    <T extends BaseModel> Stream<T> getObjects(
            Class<? extends BaseModel> fromClass, long fromId,
            Class<T> clazz, Set<Class<? extends BaseModel>> proxies, boolean forward) {

        CacheNode rootNode = nodes.get(new CacheKey(fromClass, fromId));
        if (rootNode != null) {
            return getObjectStream(rootNode, clazz, proxies, forward);
        } else {
            return Stream.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseModel> Stream<T> getObjectStream(
            CacheNode rootNode, Class<T> clazz, Set<Class<? extends BaseModel>> proxies, boolean forward) {

        if (proxies.contains(clazz)) {
            return Stream.empty();
        }

        var directSteam = rootNode.getLinks(clazz, forward).stream()
                .map(node -> (T) node.getValue());

        var proxyStream = proxies.stream()
                .flatMap(proxyClass -> rootNode.getLinks(proxyClass, forward).stream()
                        .flatMap(node -> getObjectStream(node, clazz, proxies, forward)));

        return Stream.concat(directSteam, proxyStream);
    }

    void updateObject(BaseModel value) {
        CacheNode node = nodes.get(new CacheKey(value));
        if (node != null) {
            node.setValue(value);
        }
    }

    boolean addLink(
            Class<? extends BaseModel> fromClazz, long fromId,
            BaseModel toValue) {
        boolean stop = true;
        CacheNode fromNode = nodes.get(new CacheKey(fromClazz, fromId));
        if (fromNode != null) {
            CacheKey toKey = new CacheKey(toValue);
            CacheNode toNode = nodes.get(toKey);
            if (toNode == null) {
                stop = false;
                toNode = new CacheNode(toValue);
                nodes.put(toKey, toNode);
            }
            fromNode.getLinks(toValue.getClass(), true).add(toNode);
            toNode.getLinks(fromClazz, false).add(fromNode);
        }
        return stop;
    }

    void removeLink(
            Class<? extends BaseModel> fromClazz, long fromId,
            Class<? extends BaseModel> toClazz, long toId) {
        CacheNode fromNode = nodes.get(new CacheKey(fromClazz, fromId));
        if (fromNode != null) {
            CacheNode toNode = nodes.get(new CacheKey(toClazz, toId));
            if (toNode != null) {
                fromNode.getLinks(toClazz, true).remove(toNode);
                toNode.getLinks(fromClazz, false).remove(fromNode);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (CacheNode node : roots.values()) {
            printNode(stringBuilder, node, "");
        }
        return stringBuilder.toString().trim();
    }

    private void printNode(StringBuilder stringBuilder, CacheNode node, String indentation) {
        stringBuilder
                .append('\n')
                .append(indentation)
                .append(node.getValue().getClass().getSimpleName())
                .append('(').append(node.getValue().getId()).append(')');
        node.getAllLinks(true).forEach(child -> printNode(stringBuilder, child, indentation + "  "));
    }

}
