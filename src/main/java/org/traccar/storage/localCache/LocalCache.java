package org.traccar.storage.localCache;

import jakarta.inject.Singleton;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Singleton
public class LocalCache {
    private final Cache<String, Object> cache;

    public LocalCache() {
        cache = Caffeine.newBuilder()
                .maximumSize(10_000)              // Maximum number of entries
                .expireAfterWrite(5, TimeUnit.MINUTES)  // Entry expiration time
                .recordStats()                    // Optional: enables statistics
                .build();
    }

    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public Object getOrCompute(String key, Function<String, Object> mappingFunction) {
        return cache.get(key, mappingFunction);
    }


}
