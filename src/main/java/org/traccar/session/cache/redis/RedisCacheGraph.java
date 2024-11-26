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
package org.traccar.session.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.BaseModel;
import org.traccar.session.cache.CacheGraph;
import org.traccar.session.cache.CacheKey;
import org.traccar.session.cache.CacheNode;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class RedisCacheGraph implements CacheGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheGraph.class);

    private static final String ROOTS = "roots";
    private static final String LINKS_SUFFIX = ":links";
    private static final String BACKLINKS_SUFFIX = ":backlinks";
    private static final String NODES = "nodes";
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RedisCacheGraph(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addObject(BaseModel value) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = new CacheKey(value).toString();
            String className = value.getClass().getName();
            String val = className + ":" + objectMapper.writeValueAsString(value);
            jedis.hset(ROOTS, key, val);
            jedis.hset(NODES, key, val);
            //log key
            LOGGER.info("Added object to cache: {} in class {}", key, this.getClass().getSimpleName());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeObject(Class<? extends BaseModel> clazz, long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = new CacheKey(clazz, id).toString();

            // Remove forward links
            Set<String> forwardLinks = jedis.smembers(key + LINKS_SUFFIX);
            for (String link : forwardLinks) {
                jedis.srem(link + BACKLINKS_SUFFIX, key);
            }
            jedis.del(key + LINKS_SUFFIX);

            // Remove backward links
            Set<String> backwardLinks = jedis.smembers(key + BACKLINKS_SUFFIX);
            for (String link : backwardLinks) {
                jedis.srem(link + LINKS_SUFFIX, key);
            }
            jedis.del(key + BACKLINKS_SUFFIX);

            jedis.hdel(ROOTS, key);
            jedis.hdel(NODES, key);

            // Log key
            LOGGER.info("Removed object and its links from cache: {} in class {}", key,
                    this.getClass().getSimpleName());
        }
    }

    @Override
    public <T extends BaseModel> T getObject(Class<T> clazz, long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = new CacheKey(clazz, id).toString();
            String data = jedis.hget(NODES, key);
            if (data != null) {
                String[] parts = data.split(":", 2);
                String className = parts[0];
                String serializedData = parts[1];
                if (clazz.isAssignableFrom(Class.forName(className))) {
                    //log serializedData
                    LOGGER.info("Deserialized object from cache: {} in class {}", serializedData,
                            this.getClass().getSimpleName());
                    return objectMapper.readValue(serializedData, clazz);
                } else {
                    throw new RuntimeException("Deserialized object is not of expected type: " + clazz.getName());
                }
            }
        } catch (ClassNotFoundException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public <T extends BaseModel> Stream<T> getObjects(Class<? extends BaseModel> fromClass, long fromId, Class<T> clazz,
                                                      Set<Class<? extends BaseModel>> proxies, boolean forward) {
        try (Jedis jedis = jedisPool.getResource()) {
            String fromKey = new CacheKey(fromClass, fromId).toString();
            Set<String> linkedKeys = jedis.smembers(fromKey + (forward ? LINKS_SUFFIX : BACKLINKS_SUFFIX));


            return linkedKeys.stream()
                    .map(linkedKey -> {
                        String data = jedis.hget(NODES, linkedKey);
                        if (data != null) {
                            String[] parts = data.split(":", 2);
                            String className = parts[0];
                            try {
                                if (clazz.isAssignableFrom(Class.forName(className))) {
                                    return objectMapper.readValue(parts[1], clazz);
                                }
                            } catch (ClassNotFoundException | JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .map(clazz::cast);
        }
    }

    @SuppressWarnings("unused")
    private <T extends BaseModel> Stream<T> getObjectStream(
            CacheNode rootNode, Class<T> clazz, Set<Class<? extends BaseModel>> proxies, boolean forward) {

        try (Jedis jedis = jedisPool.getResource()) {
            String rootKey = new CacheKey(rootNode.getValue()).toString();
            String linkSuffix = forward ? LINKS_SUFFIX : BACKLINKS_SUFFIX;

            // Direct links
            Set<String> directLinks = jedis.smembers(rootKey + linkSuffix);
            Stream<T> directStream = directLinks.stream()
                    .map(link -> jedis.hget(NODES, link))
                    .filter(Objects::nonNull)
                    .map(json -> {
                        try {
                            String[] parts = json.split(":", 2);
                            String className = parts[0];
                                if (clazz.isAssignableFrom(Class.forName(className))) {
                                    return objectMapper.readValue(parts[1], clazz);
                                }
                            return null;
                        } catch (JsonProcessingException e) {
                            LOGGER.error("Error deserializing object", e);
                            return null;
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(Objects::nonNull);

            // Proxy links
            var proxyStream = proxies.stream()
                    .flatMap(proxyClass -> {
                        Set<String> proxyLinks = jedis.smembers(rootKey + linkSuffix);
                        return proxyLinks.stream()
                                .flatMap(link -> {
                                    try {
                                        CacheNode proxyNode = new CacheNode(objectMapper.readValue(
                                                jedis.hget(NODES, link).split(":", 2)[1], proxyClass));
                                        return getObjectStream(proxyNode, clazz, proxies, forward);
                                    } catch (JsonProcessingException e) {
                                        LOGGER.error("Error deserializing proxy object", e);
                                        return Stream.empty();
                                    }
                                });
                    });

            return Stream.concat(directStream, proxyStream);
        }
    }

    @Override
    public void updateObject(BaseModel value) {
        addObject(value);
    }

    @Override
    public boolean addLink(Class<? extends BaseModel> fromClazz, long fromId, BaseModel toValue) {
        try (Jedis jedis = jedisPool.getResource()) {
            boolean stop = true;

            String fromKey = new CacheKey(fromClazz, fromId).toString();
            String toKey = new CacheKey(toValue).toString();

            if (!jedis.hexists(NODES, toKey)) {
                String className = toValue.getClass().getName();
                String valueCache;
                try {
                    valueCache = className + ":" + objectMapper.writeValueAsString(toValue);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                jedis.hset(NODES, toKey, valueCache);
                stop = false;
            }
            // Add the forward and backward links
            jedis.sadd(fromKey + LINKS_SUFFIX, toKey);
            jedis.sadd(toKey + BACKLINKS_SUFFIX, fromKey);

            // Log the addition of the link
            LOGGER.info("Added link from {} to {} in class {}", fromKey, toKey, this.getClass().getSimpleName());
            return stop;
        }
    }

    @Override
    public void removeLink(Class<? extends BaseModel> fromClazz, long fromId, Class<? extends BaseModel> toClazz,
                           long toId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String fromKey = new CacheKey(fromClazz, fromId).toString();
            String toKey = new CacheKey(toClazz, toId).toString();
            jedis.srem(fromKey + LINKS_SUFFIX, toKey);
            jedis.srem(toKey + BACKLINKS_SUFFIX, fromKey);
            //log fromKey, toKey
            LOGGER.info("Removed link from {} to {} in class {}", fromKey, toKey, this.getClass().getSimpleName());
        }
    }
}
